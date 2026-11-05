
pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot'
        MAVEN_HOME = 'C:\\Program Files\\apache-maven-3.9.16'
        GITLEAKS = 'C:\\Users\\Admin\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gitleaks.Gitleaks_Microsoft.Winget.Source_8wekyb3d8bbwe\\gitleaks.exe'

        DB_URL = 'jdbc:postgresql://localhost:5433/rentrolldb'
        DB_USERNAME = 'rentroll'
        DB_PASSWORD = 'rentroll123'

        PATH = "${JAVA_HOME}\\bin;${MAVEN_HOME}\\bin;${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Secret Scan') {
            steps {
                bat '"%GITLEAKS%" version'
                bat '"%GITLEAKS%" git .'
            }
        }

        stage('Build and Test') {
            steps {
                bat 'mvn --batch-mode clean verify'
            }
        }
        stage('Docker Build') {
            steps {
                bat 'docker build -t rentroll:%BUILD_NUMBER% .'
            }
        }
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_TOKEN'
                )]) {
                    bat '''
                        docker login -u %DOCKER_USER% -p %DOCKER_TOKEN%
                        docker tag rentroll:%BUILD_NUMBER% %DOCKER_USER%/rentroll:%BUILD_NUMBER%
                        docker push %DOCKER_USER%/rentroll:%BUILD_NUMBER%
                    '''
                }
            }
        }
        stage('Test AKS Connection') {
            steps {
                withCredentials([file(
                    credentialsId: 'aks-kubeconfig',
                    variable: 'KUBECONFIG_FILE'
                )]) {
                    bat '''
                        kubectl --kubeconfig "%KUBECONFIG_FILE%" get nodes
                        kubectl --kubeconfig "%KUBECONFIG_FILE%" get pods -n rentroll
                    '''
                }
            }
        }
        

        stage('Deploy to AKS using Ansible') {
            steps {
                bat '''
                    echo Deploying RentRoll build %BUILD_NUMBER% using Ansible...

                    wsl -d Ubuntu-22.04 -u srsp -- bash -lc "cd /mnt/d/source/projects/RentRoll/ansible && export ANSIBLE_CONFIG=/mnt/d/source/projects/RentRoll/ansible/ansible.cfg && /home/srsp/.local/bin/ansible-playbook -i inventory/hosts.ini playbooks/deploy.yml -e image_tag=%BUILD_NUMBER%"
                '''
            }
        }


    }

    post {
        success {
            echo 'RentRoll CI completed successfully!'
        }
        failure {
            echo 'RentRoll CI failed. Check the stage logs.'
        }
    }
}
