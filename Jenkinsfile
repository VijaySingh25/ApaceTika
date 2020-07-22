#!/usr/bin/env groovy

@Library('jenkins-shared-library') _

/**
 * Jenkinsfile Scripted pipeline
 */

CREDENTIALS_GITHUB = 'jenkins-ssh-git'
def repositoryOwner = "AppDirect"
def repositoryName = "apache-tika"
def PR_BRANCH_REGEX = /(pr|PR)-\d+$/
def DEVELOP_BRANCH_REGEX = /develop/
def RELEASE_BRANCH_REGEX = /release/
def MASTER_BRANCH_REGEX = /master/

def slackChannel = "#psds-build-alerts"
def slackUser = ""

def SONAR_SERVER_NAME = 'Sonar v2'
def CREDENTIALS_SONAR_GITHUB = "sonar-github-token-text"

def version

pipeline {
    agent {
        docker {
            image 'docker.appdirect.tools/appdirect/build-jdk11:1.0.126'
            args  '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    stages {

        stage('Prep') {
            steps {
                script {
                    currentBuild.result = 'SUCCESS'
                    env.COMMIT = sh(script: 'git show -s --format=%H', returnStdout: true).trim()
                    env.COMMIT_SUBJECT = sh(script: 'git show -s --format=%s', returnStdout: true).trim()
                    env.CHANGE_URL = "https://github.com/AppDirect/tika/pull/${env.CHANGE_ID}"
                    env.CHANGE_TITLE = "${env.BRANCH_NAME}"
                    slackUser = '@' + sh(script: 'git show -s --format=%ae | cut -d@ -f1', returnStdout: true).trim()
                }
            }
        }

        stage('Checkout') {
            steps {
                echo 'Checking out from repository...'
                checkout scm: [
                        $class           : 'GitSCM',
                        branches         : scm.branches,
                        userRemoteConfigs: scm.userRemoteConfigs,
                        extensions       : [
                                [$class: 'CloneOption', noTags: false],
                                [$class: 'LocalBranch', localBranch: "**"]
                        ]
                ]
                script {
                    echo sh(returnStdout: true, script: 'env')
					version = getSemver('master', '', env.BRANCH_NAME != 'master' ? '-SNAPSHOT' : '')
                }
            }
        }

        stage('Build') {
            steps {
                echo 'Prepare Gradle properties'
                sh "echo version=${version} >> gradle.properties"

                echo 'Building project without tests...'
                sh "./gradlew clean build -x test"
            }
        }

        stage('Run Tests') {
            parallel{

                stage('Unit Tests') {
                    steps {
                        sh './gradlew unitTest'
                    }
                    post {
                        always {
                            junit 'test/build/test-results/unitTest/*.xml'

                            publishHTML([allowMissing: false, alwaysLinkToLastBuild: false,
                                         keepAll: false,
                                         reportDir: 'test/build/reports/tests/unitTest',
                                         reportFiles: 'index.html',
                                         reportName: 'UnitTestReport',
                                         reportTitles: 'UnitTestsReport'])
                        }
                    }
                }

                stage('Integration Tests'){
                    steps{
                        sh './gradlew integrationTest'
                    }
                    post{
                        always{
                            junit 'test/build/test-results/integrationTest/*.xml'

                            publishHTML([allowMissing: false, alwaysLinkToLastBuild: false,
                                         keepAll: false,
                                         reportDir: 'test/build/reports/tests/integrationTest',
                                         reportFiles: 'index.html',
                                         reportName: 'IntegrationTestReport',
                                         reportTitles: 'IntegrationTestsReport'])
                        }
                    }
                }
            }
        }

        stage('Code Analysis') {
            environment {
                githubToken = credentials('sonar-github-token')
            }
            steps {
                script {
                    props = readProperties file: 'gradle.properties'
                    //sh "rm test/build/jacoco/componentTest.exec"
                }
                echo "Running code coverage..."
                sh "./gradlew UTcoverage -Pjacoco.url=${BUILD_URL}jacoco"

                sh "./gradlew ITcoverage -Pjacoco.url=${BUILD_URL}jacoco"

                sh "./gradlew coverageReport -Pjacoco.url=${BUILD_URL}jacoco"
                sh "curl -H 'Authorization: token ${githubToken}' -H 'Content-Type: application/json' -X POST -d @./build/reports/jacoco/coverage.json https://api.github.com/repos/${repositoryOwner}/${repositoryName}/issues/${env.CHANGE_ID}/comments"

                step([$class          : 'JacocoPublisher',
                        execPattern     : 'test/build/jacoco/*.exec',
                        inclusionPattern: props['jacoco.include']
                ])
            }
        }

        stage('SonarQube') {
            steps {
                sonarScanner version
            }
        }

        stage('Push Artifacts') {
            environment {
                dockerRegistry = credentials('docker-rw')
                artifactoryAuth = credentials('jenkins-artifactory-credentials')
            }
            steps {
                script {
                    prNumberProperty = env.BRANCH_NAME =~ PR_BRANCH_REGEX ? "-PprNumber=\"${env.BRANCH_NAME}\"" : ""
                    dockerTagOverride = env.CHANGE_TARGET == null && env.BRANCH_NAME =~ MASTER_BRANCH_REGEX ? "-PdockerTagVersion=latest" : ""
                }
                sh "./gradlew publishDockerImage -PregistryUser=${dockerRegistry_USR} -PregistryPass=${dockerRegistry_PSW} ${dockerTagOverride}"

                echo 'Publish Swagger YAML file and application jar...'
                sh "./gradlew publish -PartifactoryUser=${artifactoryAuth_USR} -PartifactoryPass=${artifactoryAuth_PSW} ${dockerTagOverride}"

                script {
                    withMasterBranch {
                        sh "./gradlew publishDockerImage -PregistryUser=${dockerRegistry_USR} -PregistryPass=${dockerRegistry_PSW}"
                        echo 'Create smoke test jar'
                        sh './gradlew buildSmokeTestJar'
                        echo 'Publishing smoke tests artifact...'
                        sh "./gradlew publishSmokeTestDockerImage -PregistryUser=${dockerRegistry_USR} -PregistryPass=${dockerRegistry_PSW}"
                    }
                }
            }
            post {
                success {
                    slackBuildStatus slackChannel, slackUser
                }
            }
        }

        stage('Release Scope') {
            steps {
                withMasterBranch {
                    pushGitTag version
                }
            }
        }
    }
}
