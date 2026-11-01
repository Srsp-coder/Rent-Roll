/**
 * RentRoll - Frontend UI Enhancements & Interactions
 */
document.addEventListener('DOMContentLoaded', function () {
    // 1. Mobile Sidebar Toggle
    const sidebarToggleBtn = document.getElementById('sidebarToggleBtn');
    const appSidebar = document.getElementById('appSidebar');

    if (sidebarToggleBtn && appSidebar) {
        let backdrop = null;

        function closeSidebar() {
            appSidebar.classList.remove('show');
            if (backdrop) {
                backdrop.remove();
                backdrop = null;
            }
        }

        sidebarToggleBtn.addEventListener('click', function () {
            const isOpen = appSidebar.classList.toggle('show');
            if (isOpen) {
                backdrop = document.createElement('div');
                backdrop.className = 'sidebar-backdrop';
                backdrop.addEventListener('click', closeSidebar);
                document.body.appendChild(backdrop);
            } else {
                closeSidebar();
            }
        });
    }

    // 2. Password Visibility Toggle
    const togglePasswordBtn = document.getElementById('togglePasswordBtn');
    const passwordInput = document.getElementById('password');

    if (togglePasswordBtn && passwordInput) {
        togglePasswordBtn.addEventListener('click', function () {
            const isPassword = passwordInput.getAttribute('type') === 'password';
            passwordInput.setAttribute('type', isPassword ? 'text' : 'password');
            this.innerHTML = isPassword ? '<i class="bi bi-eye-slash"></i>' : '<i class="bi bi-eye"></i>';
        });
    }
});

/**
 * Helper to auto-fill credentials on the login screen
 */
function fillLoginCredentials(email, password) {
    const emailField = document.getElementById('email');
    const passwordField = document.getElementById('password');
    if (emailField && passwordField) {
        emailField.value = email;
        passwordField.value = password;
        emailField.focus();
    }
}

