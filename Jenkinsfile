pipeline {
  agent any

  triggers {
    githubPush()
  }

  environment {
    CHROME_HEADLESS = 'true'
  }

  options {
    timestamps()
    timeout(time: 40, unit: 'MINUTES')
  }

  stages {

    stage('0- Agent / Tool Check') {
      steps {
        script {
          if (isUnix()) {
            sh 'uname -a || true'
            sh 'which docker || true'
            sh 'docker --version || true'
            sh 'docker compose version || true'
          } else {
            bat 'ver'
            bat 'where docker || echo docker_not_found'
            bat 'docker --version || echo docker_not_found'
            bat 'docker compose version || echo compose_not_found'
          }
        }
      }
    }

    stage('1- Checkout') {
      steps {
        checkout scm
      }
    }

    stage('2- Build (Maven)') {
      steps {
        script {
          if (isUnix()) {
            sh 'chmod +x mvnw || true'
            sh './mvnw -DskipTests clean package'
          } else {
            // Windows
            bat 'mvnw.cmd -DskipTests clean package'
          }
        }
      }
    }

    stage('3- Unit Tests') {
      steps {
        script {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            if (isUnix()) {
              // Sadece unit test
              sh './mvnw -DskipITs=true test'
            } else {
              bat 'mvnw.cmd -DskipITs=true test'
            }
          }
        }
      }
      post {
        always {
          // Surefire raporları
          junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
      }
    }

    stage('4- Integration Tests') {
      steps {
        script {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            if (isUnix()) {
              // Sadece `com.hospital.automation.integration` paketindeki integration testleri çalıştır
              sh './mvnw -DskipUTs=true -Dit.test="com.hospital.automation.integration.*IT" verify'
            } else {
              // Windows: aynı paket filtresiyle çalıştır
              bat 'mvnw.cmd -DskipUTs=true -Dit.test="com.hospital.automation.integration.*IT" verify'
            }
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
        }
      }
    }

    stage('5- Docker Compose Up') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              docker compose down -v || true
              docker compose up -d --build

              echo "Waiting for backend..."
              for i in {1..60}; do
                if curl -sf --connect-timeout 5 http://localhost:9060/ui; then
                  echo "Backend is ready!"
                  exit 0
                fi
                echo "Not ready yet... ($i/60)"
                sleep 5
              done

              echo "Backend did not become ready in time."
              exit 1
            '''
          } else {
            // Windows: Docker path gerekiyorsa sadece bu stage'de PATH'e ekle
            withEnv(["PATH=C:\\Program Files\\Docker\\Docker\\resources\\bin;${env.PATH}"]) {
              bat '''
                docker compose down -v || exit /b 0
                docker compose up -d --build
              '''

              // Windows'ta curl yoksa PowerShell ile kontrol:
              powershell '''
                Write-Host "Waiting for backend..."
                $ok = $false
                for ($i=1; $i -le 60; $i++) {
                  try {
                    $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 http://localhost:9060/ui
                    if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { $ok = $true; break }
                  } catch {}
                  Start-Sleep -Seconds 5
                }
                if (-not $ok) { throw "Backend did not become ready in time." }
                Write-Host "Backend is ready!"
              '''
            }
          }
        }
      }
    }

    // Yeni stage: Selenium standalone Chrome container başlat
    stage('5.5- Start Selenium Container') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              docker rm -f selenium_chrome || true
              docker run -d --rm -p 4444:4444 --shm-size=2g --name selenium_chrome selenium/standalone-chrome:latest

              echo "Waiting for selenium..."
              for i in {1..30}; do
                if curl -sf --connect-timeout 5 http://localhost:4444/status; then
                  echo "Selenium is ready!"
                  exit 0
                fi
                echo "Selenium not ready yet... ($i/30)"
                sleep 2
              done

              echo "Selenium did not become ready in time."
              exit 1
            '''
          } else {
            withEnv(["PATH=C:\\Program Files\\Docker\\Docker\\resources\\bin;${env.PATH}"]) {
              bat '''
                docker rm -f selenium_chrome || exit /b 0
                docker run -d --rm -p 4444:4444 --shm-size=2g --name selenium_chrome selenium/standalone-chrome:latest
              '''

              powershell '''
                Write-Host "Waiting for selenium..."
                $ok = $false
                for ($i=1; $i -le 30; $i++) {
                  try {
                    $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 http://localhost:4444/status
                    if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { $ok = $true; break }
                  } catch {}
                  Start-Sleep -Seconds 2
                }
                if (-not $ok) { throw "Selenium did not become ready in time." }
                Write-Host "Selenium is ready!"
              '''
            }
          }
        }
      }
    }


    stage('6- Selenium - Appointment') {
      steps {
        script {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            if (isUnix()) {
              withEnv(["SELENIUM_REMOTE_URL=http://localhost:4444"]){
                sh './mvnw -DskipUTs=true -Dit.test="com.hospital.automation.selenium.AppointmentSeleniumIT" verify'
              }
            } else {
              withEnv(["SELENIUM_REMOTE_URL=http://localhost:4444"]) {
                bat 'mvnw.cmd -DskipUTs=true -Dit.test="com.hospital.automation.selenium.AppointmentSeleniumIT" verify'
              }
            }
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
        }
      }
    }

    stage('7- Selenium - Patient') {
      steps {
        script {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            if (isUnix()) {
              withEnv(["SELENIUM_REMOTE_URL=http://localhost:4444"]){
                sh './mvnw -DskipUTs=true -Dit.test="com.hospital.automation.selenium.PatientSeleniumIT" verify'
              }
            } else {
              withEnv(["SELENIUM_REMOTE_URL=http://localhost:4444"]) {
                bat 'mvnw.cmd -DskipUTs=true -Dit.test="com.hospital.automation.selenium.PatientSeleniumIT" verify'
              }
            }
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
        }
      }
    }

    stage('8- Selenium - Doctor') {
      steps {
        script {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            if (isUnix()) {
              withEnv(["SELENIUM_REMOTE_URL=http://localhost:4444"]){
                sh './mvnw -DskipUTs=true -Dit.test="com.hospital.automation.selenium.DoctorSeleniumIT" verify'
              }
            } else {
              withEnv(["SELENIUM_REMOTE_URL=http://localhost:4444"]) {
                bat 'mvnw.cmd -DskipUTs=true -Dit.test="com.hospital.automation.selenium.DoctorSeleniumIT" verify'
              }
            }
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
        }
      }
    }

    stage('9- Selenium - Department') {
      steps {
        script {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            if (isUnix()) {
              withEnv(["SELENIUM_REMOTE_URL=http://localhost:4444"]){
                sh './mvnw -DskipUTs=true -Dit.test="com.hospital.automation.selenium.DepartmentSeleniumIT" verify'
              }
            } else {
              withEnv(["SELENIUM_REMOTE_URL=http://localhost:4444"]) {
                bat 'mvnw.cmd -DskipUTs=true -Dit.test="com.hospital.automation.selenium.DepartmentSeleniumIT" verify'
              }
            }
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
        }
      }
    }

    stage('10- JaCoCo Report') {
      steps {
        script {
          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            if (isUnix()) {
              // Pom.xml'de jacoco plugin varsa çalışır
              sh './mvnw jacoco:report'
            } else {
              bat 'mvnw.cmd jacoco:report'
            }
          }
        }
      }
      post {
        always {
          echo 'JaCoCo üretildiyse genelde: target/site/jacoco/ altında olur.'
        }
      }
    }
  }

  post {
    always {
      script {
        if (isUnix()) {
          sh 'docker compose down -v || true'
          sh 'docker rm -f selenium_chrome || true'
        } else {
          withEnv(["PATH=C:\\ Program Files\\Docker\\Docker\\resources\\bin;${env.PATH}"]) {
            bat 'docker compose down -v || exit /b 0'
            bat 'docker rm -f selenium_chrome || exit /b 0'
          }
        }
      }
    }
  }
}
