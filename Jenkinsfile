
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

        stage('DinD Experiment') {
            steps {
                catchError(
                    buildResult: 'SUCCESS',
                    stageResult: 'UNSTABLE'
                ) {
                    script {
                        def name = "rentroll-dind-${BUILD_NUMBER}"

                        try {
                            bat """
                                echo ===== STARTING DIND =====

                                docker run -d --privileged --name ${name} docker:29-dind

                                if errorlevel 1 exit /b 1

                                echo ===== WAITING FOR INNER DOCKER DAEMON =====

                                docker exec ${name} sh -c "for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30; do docker info >/dev/null 2>&1 && exit 0; sleep 2; done; exit 1"

                                if errorlevel 1 exit /b 1

                                echo ===== PULLING NGINX =====

                                docker exec ${name} docker pull nginx

                                if errorlevel 1 exit /b 1

                                echo ===== RUNNING INNER NGINX =====

                                docker exec ${name} docker run -d --name nginx-child nginx

                                if errorlevel 1 exit /b 1

                                echo ===== INNER CONTAINER SIZE =====

                                docker exec ${name} docker ps -s

                                echo ===== INNER IMAGE STORAGE =====

                                docker exec ${name} docker system df -v

                                echo ===== INNER DOCKER STORAGE DIRECTORY =====

                                docker exec ${name} du -sh /var/lib/docker

                                echo ===== OUTER DIND RESOURCE USAGE =====

                                docker stats --no-stream ${name}

                                echo ===== OUTER CONTAINER SIZE =====

                                docker ps -s --filter "name=${name}"
                            """
                        }
                        finally {
                            echo 'Cleaning up temporary DinD container...'

                            bat "docker rm -fv ${name} >nul 2>&1 || exit /b 0"
                        }
                    }
                }
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
