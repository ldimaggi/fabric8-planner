#!/usr/bin/groovy
def ci (project){
    def tempVersion

    stage('Setup & Build'){
        container('ui'){
            sh 'npm install'
            sh 'npm run build'
            sh 'npm pack dist/'
        }
    }

    stage('Build fabric8-ui'){
        container('ui'){
            tempVersion = buildSnapshotFabric8UI{
                pullRequestProject = project
            }
        }
    }

    stage('Unit Tests'){
        container('ui'){
            sh 'npm run tests -- --unit'
        }
    }

    stage('Functional Tests'){
        dir('runtime'){
            container('ui'){
                sh '''
        npm cache clean --force
        npm install
        cd src/tests/functionalTests
        DEBUG=true HEADLESS_MODE=true ./run_ts_functional_tests.sh smokeTest
'''
            }
        }
    }

    return tempVersion
}

def buildImage(imageName){
    stage('Snapshot Image'){
        sh "cd fabric8-ui && docker build -t ${imageName} -f Dockerfile.deploy ."
        sh "cd fabric8-ui && docker push ${imageName}"
    }
}

def cd (b){
    stage('Repo Fix'){
        sh './scripts/fix-git-repo.sh'
    }

    stage('Setup & Build'){
        sh 'npm install'
        sh 'npm run build'
    }

    stage('Unit Tests'){
        sh 'npm run tests -- --unit'
    }

    stage('Functional Tests'){
        dir('runtime'){
            container('ui'){
                sh '''
        npm cache clean --force
        npm install
        cd src/tests/functionalTests
        DEBUG=true HEADLESS_MODE=true ./run_ts_functional_tests.sh smokeTest
        '''
            }
        }
    }

    stage('Release'){
        def published = npmRelease{
            branch = b
        }
        return published
    }
}

def updateDownstreamProjects(v){
    echo 'we would Update Downstream Projects'
    pushPackageJSONChangePR{
        propertyName = 'fabric8-planner'
        projects = [
                'fabric8-ui/fabric8-npm-dependencies'
        ]
        version = v
        containerName = 'ui'
        autoMerge = true
    }
}
return this
