def name = 'test-1'
def giturl = 'https://github.com/wcd923/spring-boot-web-app.git'
def branch = 'master' //ALL
def creds = "admin"
def poll = 'H/15 * * * *'

job(name){
    description("Test job created on ${new Date()} named: ${name}")
    logRotator{
        numToKeep 50
    }

    parameters {
        stringParam('MESSAGE', 'Hello world!')
    }

    scm {
        git(giturl,branch)
    }


    triggers {
        scm('@daily')
    }

    steps{
        print(MESSAGE)
        shell('grep -v "#" variables.txt > variables_origin.txt')
        shell('''sed -i 's/=/&"/' variables_origin.txt''')
        shell('''sed -i /=/s/$/'"'/ variables_origin.txt''')

        maven{
            mavenInstallation('M3')
            goals('mvn -B package -Dmaven.test.failure.ignore')

        }
    }


}