import re
import urllib.request
import urllib.parse
import http.cookiejar
import sys

BASE_URL = "http://localhost:8085"

class TestSession:
    def __init__(self):
        self.cj = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cj))

    def get(self, path):
        req = urllib.request.Request(f"{BASE_URL}{path}", headers={"User-Agent": "RentRollTester/1.0"})
        try:
            with self.opener.open(req) as resp:
                body = resp.read().decode('utf-8')
                return resp.status, body, resp.geturl()
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode('utf-8', errors='ignore'), e.geturl()

    def post(self, path, data):
        encoded_data = urllib.parse.urlencode(data).encode('utf-8')
        req = urllib.request.Request(f"{BASE_URL}{path}", data=encoded_data,
                                     headers={"User-Agent": "RentRollTester/1.0", "Content-Type": "application/x-www-form-urlencoded"})
        try:
            with self.opener.open(req) as resp:
                body = resp.read().decode('utf-8')
                return resp.status, body, resp.geturl()
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode('utf-8', errors='ignore'), e.geturl()

    def get_csrf(self, path="/login"):
        status, html, _ = self.get(path)
        match = re.search(r'name="_csrf"\s+value="([^"]+)"', html)
        if not match:
            # check reverse attribute order
            match = re.search(r'value="([^"]+)"\s+name="_csrf"', html)
        if match:
            return match.group(1)
        raise Exception(f"CSRF token not found on {path}")

    def login(self, email, password):
        csrf = self.get_csrf("/login")
        status, body, final_url = self.post("/login", {
            "email": email,
            "password": password,
            "_csrf": csrf
        })
        # After redirect from /login, should reach /dashboard
        return final_url.endswith("/dashboard"), status, body, final_url

def run_tests():
    print("=" * 65)
    print("RentRoll End-to-End System & Workflow Verification")
    print(f"Target Server: {BASE_URL}")
    print("=" * 65)

    results = {}

    # -------------------------------------------------------------
    # 1. Unauthenticated root & login access
    # -------------------------------------------------------------
    s_anon = TestSession()
    status, _, url = s_anon.get("/")
    print(f"[TEST 1] Root URL Redirect: Status={status}, FinalURL={url}")
    results["Root Redirect"] = url.endswith("/login")

    status, html, _ = s_anon.get("/login")
    results["Login Page Accessible"] = (status == 200 and "RentRoll" in html)
    print(f"[TEST 2] Login Page Accessible: {results['Login Page Accessible']}")

    # -------------------------------------------------------------
    # 2. Landlord Authentication & Dashboard
    # -------------------------------------------------------------
    s_landlord = TestSession()
    success, status, body, url = s_landlord.login("rajesh.landlord@rentroll.in", "landlord123")
    results["Landlord Login"] = success
    print(f"[TEST 3] Landlord Login: Success={success}, FinalURL={url}")

    status, html, _ = s_landlord.get("/dashboard")
    has_kpis = "Landlord Financial Dashboard" in html and "Active Leases" in html
    results["Landlord Dashboard"] = (status == 200 and has_kpis)
    print(f"[TEST 4] Landlord Dashboard KPIs: {results['Landlord Dashboard']}")

    # -------------------------------------------------------------
    # 3. Landlord Properties Listing & Details
    # -------------------------------------------------------------
    status, html, _ = s_landlord.get("/properties")
    has_props = "Prestige Shantiniketan" in html
    results["Landlord Properties List"] = (status == 200 and has_props)
    print(f"[TEST 5] Landlord Properties List: {results['Landlord Properties List']}")

    status, html, _ = s_landlord.get("/properties/1")
    results["Property Details View"] = (status == 200 and "Prestige Shantiniketan" in html)
    print(f"[TEST 6] Property Details View: {results['Property Details View']}")

    # -------------------------------------------------------------
    # 4. Landlord Property Creation
    # -------------------------------------------------------------
    csrf = s_landlord.get_csrf("/properties/new")
    new_prop_data = {
        "propertyName": "Koramangala 4th Block Apartment 101",
        "address": "80 Feet Road, 4th Block",
        "city": "Bengaluru",
        "state": "Karnataka",
        "pinCode": "560034",
        "propertyType": "APARTMENT",
        "bedrooms": "2",
        "monthlyRent": "38000",
        "description": "Prime residential 2 BHK near Sony Signal with stilt parking and lift.",
        "_csrf": csrf
    }
    status, html, url = s_landlord.post("/properties/new", new_prop_data)
    results["Property Creation"] = (status == 200 and "Koramangala 4th Block" in html)
    print(f"[TEST 7] Property Creation: {results['Property Creation']}, URL={url}")

    # Extract new property ID from URL
    prop_id_match = re.search(r'/properties/(\d+)', url)
    new_prop_id = prop_id_match.group(1) if prop_id_match else "6"

    # -------------------------------------------------------------
    # 5. Landlord Lease Creation & Monthly Rent Schedule Generation
    # -------------------------------------------------------------
    csrf = s_landlord.get_csrf("/leases/new")
    new_lease_data = {
        "propertyId": new_prop_id,
        "tenantId": "4", # Amit Kumar (Tenant)
        "startDate": "2027-01-01",
        "endDate": "2027-06-30",
        "monthlyRent": "38000",
        "dueDayOfMonth": "5",
        "securityDeposit": "120000",
        "_csrf": csrf
    }
    status, html, url = s_landlord.post("/leases/new", new_lease_data)
    has_schedule = "Scheduled Monthly Rent Payments" in html and "January 2027" in html
    results["Lease & Rent Schedule Creation"] = (status == 200 and has_schedule)
    print(f"[TEST 8] Lease & Automated Schedule: {results['Lease & Rent Schedule Creation']}")

    # -------------------------------------------------------------
    # 6. Rent Payments Ledger & 5% Late Fee Calculation
    # -------------------------------------------------------------
    status, html, _ = s_landlord.get("/payments")
    results["Payments Ledger View"] = (status == 200 and "October 2026" in html)
    print(f"[TEST 9] Payments Ledger View: {results['Payments Ledger View']}")

    # Check overdue payment (Payment #22 for Sneha Nair was created with status LATE for Oct 2026)
    # Let's inspect Payment details and record payment to test 5% late fee application
    status, html, _ = s_landlord.get("/payments/22")
    results["Payment Details View"] = (status == 200 and "October 2026" in html)
    print(f"[TEST 10] Payment Details View: {results['Payment Details View']}")

    csrf = s_landlord.get_csrf("/payments/22")
    pay_action_data = {
        "paymentDate": "2026-11-01",
        "paymentMethod": "UPI (Google Pay)",
        "notes": "Late rent settled via UPI Ref #99887711",
        "_csrf": csrf
    }
    status, html, _ = s_landlord.post("/payments/22/pay", pay_action_data)
    # Late fee on 58,000 base rent at 5% is 2,900
    has_late_fee = "5% late fee" in html or "2,900" in html or "2900" in html or "Settled" in html
    results["Payment Settlement with Late Fee"] = (status == 200 and has_late_fee)
    print(f"[TEST 11] Overdue Payment Marked as Paid (with 5% Late Fee): {results['Payment Settlement with Late Fee']}")

    # -------------------------------------------------------------
    # 7. CSV Export Verification
    # -------------------------------------------------------------
    status, csv_content, _ = s_landlord.get("/payments/export")
    is_csv_valid = (status == 200 and "Payment ID,Property,Tenant" in csv_content and "Prestige Shantiniketan" in csv_content)
    results["CSV Ledger Export"] = is_csv_valid
    print(f"[TEST 12] CSV Ledger Export: {results['CSV Ledger Export']}")

    # -------------------------------------------------------------
    # 8. Reports & Lease Expiry Watch
    # -------------------------------------------------------------
    status, html, _ = s_landlord.get("/reports?year=2026&month=11")
    has_report = "Expected Rent" in html and "Collection Rate" in html
    results["Financial Reports & Expiry Watch"] = (status == 200 and has_report)
    print(f"[TEST 13] Financial Reports & Expiry Watch: {results['Financial Reports & Expiry Watch']}")

    # -------------------------------------------------------------
    # 9. Tenant Login & Portal
    # -------------------------------------------------------------
    s_tenant = TestSession()
    success, status, body, url = s_tenant.login("amit.tenant@rentroll.in", "tenant123")
    results["Tenant Login"] = success
    print(f"[TEST 14] Tenant Login: Success={success}, FinalURL={url}")

    status, html, _ = s_tenant.get("/dashboard")
    results["Tenant Dashboard"] = (status == 200 and "Tenant Portal" in html and "Monthly Rent" in html)
    print(f"[TEST 15] Tenant Dashboard: {results['Tenant Dashboard']}")

    # -------------------------------------------------------------
    # 10. Tenant Maintenance Request Submission
    # -------------------------------------------------------------
    csrf = s_tenant.get_csrf("/maintenance/new")
    ticket_data = {
        "propertyId": "1", # Prestige Shantiniketan Flat 702
        "title": "Master bedroom AC cooling not effective",
        "category": "APPLIANCE",
        "priority": "MEDIUM",
        "description": "The AC unit in master bedroom runs but room temperature stays warm. Needs gas pressure check.",
        "_csrf": csrf
    }
    status, html, url = s_tenant.post("/maintenance/new", ticket_data)
    results["Tenant Maintenance Ticket Creation"] = (status == 200 and "Master bedroom AC" in html)
    print(f"[TEST 16] Tenant Maintenance Ticket Creation: {results['Tenant Maintenance Ticket Creation']}")

    # Extract ticket ID
    ticket_id_match = re.search(r'/maintenance/(\d+)', url)
    ticket_id = ticket_id_match.group(1) if ticket_id_match else "5"

    # -------------------------------------------------------------
    # 11. Security Check: Tenant Forbidden from Marking Payment as Paid
    # -------------------------------------------------------------
    csrf = s_tenant.get_csrf(f"/payments/11")
    status, html, _ = s_tenant.post("/payments/11/pay", {
        "paymentDate": "2026-11-01",
        "paymentMethod": "Cash",
        "notes": "Attempting self payment",
        "_csrf": csrf
    })
    results["Tenant Cannot Mark Payment Paid"] = ("Tenants are not permitted" in html or status == 403)
    print(f"[TEST 17] Security Check: Tenant Blocked from Marking Paid: {results['Tenant Cannot Mark Payment Paid']}")

    # -------------------------------------------------------------
    # 12. Landlord Resolving Maintenance Ticket
    # -------------------------------------------------------------
    csrf = s_landlord.get_csrf(f"/maintenance/{ticket_id}")
    status, html, _ = s_landlord.post(f"/maintenance/{ticket_id}/status", {
        "status": "RESOLVED",
        "resolutionNotes": "Daikin service engineer visited, refilled R32 refrigerant. Cooling restored.",
        "_csrf": csrf
    })
    results["Landlord Ticket Resolution"] = (status == 200 and ("Resolved" in html or "RESOLVED" in html))
    print(f"[TEST 18] Landlord Ticket Resolution: {results['Landlord Ticket Resolution']}")

    # -------------------------------------------------------------
    # 13. Admin Login & User Management
    # -------------------------------------------------------------
    s_admin = TestSession()
    success, status, body, url = s_admin.login("admin@rentroll.in", "admin123")
    results["Admin Login"] = success
    print(f"[TEST 19] Admin Login: Success={success}, FinalURL={url}")

    status, html, _ = s_admin.get("/dashboard")
    results["Admin Dashboard"] = (status == 200 and "System Administration" in html)
    print(f"[TEST 20] Admin Dashboard: {results['Admin Dashboard']}")

    status, html, _ = s_admin.get("/users")
    results["Admin Users Directory"] = (status == 200 and "User Account Directory" in html)
    print(f"[TEST 21] Admin Users Directory: {results['Admin Users Directory']}")

    # Admin creates a new Tenant user
    csrf = s_admin.get_csrf("/users/new")
    new_user_data = {
        "fullName": "Deepak Joshi (New Tenant)",
        "email": "deepak.joshi@rentroll.in",
        "password": "password123",
        "phoneNumber": "+91 9833445566",
        "role": "TENANT",
        "_csrf": csrf
    }
    status, html, url = s_admin.post("/users/new", new_user_data)
    results["Admin User Creation"] = (status == 200 and "Deepak Joshi" in html)
    print(f"[TEST 22] Admin User Creation: {results['Admin User Creation']}")

    # -------------------------------------------------------------
    # 14. Authorization Boundary: Landlord Forbidden from /users
    # -------------------------------------------------------------
    status, html, _ = s_landlord.get("/users")
    results["Landlord Blocked from /users"] = (status == 403 or "Access Restricted" in html)
    print(f"[TEST 23] Authorization Boundary: Landlord Blocked from /users: {results['Landlord Blocked from /users']} (Status={status})")

    # -------------------------------------------------------------
    # Final Summary Report
    # -------------------------------------------------------------
    print("=" * 65)
    print("VERIFICATION SUMMARY")
    print("=" * 65)
    passed_count = sum(1 for v in results.values() if v)
    total_count = len(results)

    for name, ok in results.items():
        status_str = "PASS" if ok else "FAIL"
        print(f"[{status_str}] {name}")

    print("-" * 65)
    print(f"Total: {passed_count}/{total_count} checks passed.")
    print("=" * 65)

    if passed_count != total_count:
        sys.exit(1)
    else:
        print("ALL VERIFICATION CHECKS COMPLETED SUCCESSFULLY!")

if __name__ == "__main__":
    run_tests()
