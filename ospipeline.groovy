pipeline {
    agent any

    stages {
        stage('Json Download from AWS') {
            steps {
                withAWS(region:'ap-south-1',credentials:'AWS') {
                    s3Download(
                        file: "availableOSVersions.json",
                        bucket:'sanyam-jenkins-bucket',
                        path: 'dev/availableOSVersions.json',
                        force: true
                    )
                }
            }
        }
        stage("Zip upload") {
            steps{
                script {
                    inputFile = input message: 'Upload Os Update Pkg Zip file.', parameters: [file(name: "$workspace/os-update-pkg.zip")]
                }
            }
        }
        stage ("Creating JSON") {
            steps {
                script{
                    def now = new Date()
                    date = (now.format("yyyyMMdd"))
                    path_name = (now.format("yyyyMMdd") + "_OS_Update" + "/" + "os-update-pkg.zip")
                    hashkey = sh(returnStdout: true, script: "md5sum '$workspace/os-update-pkg.zip' | awk '{print \$1}'")
                    sh """
                    echo '{ "'$current_version'": {"availableOSBuildFingerprint": "'$availab_version'", "fileName": "os-update-pkg.zip", "fileLocation": "'$path_name'", "hash": "$hashkey", "minApkVersion": "'$apkversion'" } }' >> availableOSVersions.json
                    """
                }
            }
        }
        stage('Upload Json to AWS') {
            steps {
                withAWS(region:'ap-south-1',credentials:'AWS') {
                    s3Upload(
                        pathStyleAccessEnabled: true,
                        payloadSigningEnabled: true,
                        file: "availableOSVersions.json",
                        bucket: 'sanyam-jenkins-bucket',
                        path: "dev/"
                    )
                }
            }
        }
        stage('Upload Zip to AWS') {
            steps {
                script{
                    withAWS(region:'ap-south-1',credentials:'AWS') {
                        def now = new Date()
                        path_name = (now.format("yyyyMMdd") + "_OS_Update")
                        s3Upload(
                            pathStyleAccessEnabled: true,
                            payloadSigningEnabled: true,
                            file: "os-update-pkg.zip",
                            bucket: 'sanyam-jenkins-bucket',
                            path: 'dev' + '/' + "$path_name" + '/'
                        )
                    }
                }
            }
        }
        stage('Workspace Cleanup') {
            steps{
                cleanWs()
            }
        }
    }
}
