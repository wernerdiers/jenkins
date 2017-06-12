The Job DSL allows you to write groovy code to describe your jobs. That means
1	Jobs are now in version control
2	You can peer review job changes
3	You can write tests for jobs(!)
4	You can make and extend classes of jobs

SEED_JOB
A seed job is a job you create in Jenkins, pulling in the repo where you keep your job scripts,
with a Build Step of Process Job DSLs. You tell that build step where your job scripts live in the workspace.
Then, you run the job. If all is well, it’ll create the jobs as you’ve configured them in your groovy script(s)
Access to the seed job is available through the SEED_JOB variable. The variable contains a reference to the internal
Jenkins object that represents the seed job. The SEED_JOB variable is only available in scripts, not in any classes
used by a script. And it is only available when running in Jenkins, e.g. in the "Process Job DSLs" build step.
The following example show how to apply the same quiet period for a generated job as for the seed job.

job('example') {
    quietPeriod(SEED_JOB.quietPeriod)
}


WORK_FLOW
Job DSL Plugin

1    Developer updates DSL scripts locally
2    Developer pushes changes
3    SCM change triggers seed job
4    Seed job runs DSL
5    Seed job updates/creates/deletes




CREATE_JOB
job(String name, Closure closure = null)          // since 1.30, an alias for freeStyleJob
freeStyleJob(String name, Closure closure = null) // since 1.30
buildFlowJob(String name, Closure closure = null) // since 1.30
ivyJob(String name, Closure closure = null)       // since 1.38
matrixJob(String name, Closure closure = null)    // since 1.30
mavenJob(String name, Closure closure = null)     // since 1.30
multiJob(String name, Closure closure = null)     // since 1.30
workflowJob(String name, Closure closure = null)  // since 1.30
multibranchWorkflowJob(String name, Closure closure = null) // since 1.42


job('DSL-1-Test') {
    scm {
        git('git://github.com/quidryan/aws-sdk-test.git')
    }
    triggers {
        scm('H/15 * * * *')
    }
    steps {
        shell("echo 'Hello World'")
        maven('-e clean test')
    }
}



CREATE_JOB_FOR_EACH_BRANCH
def project = 'quidryan/aws-sdk-test'
def branchApi = new URL("https://api.github.com/repos/${project}/branches")
def branches = new groovy.json.JsonSlurper().parse(branchApi.newReader())
branches.each {
    def branchName = it.name
    def jobName = "${project}-${branchName}".replaceAll('/','-')
    job(jobName) {
        scm {
            git("git://github.com/${project}.git", branchName)
        }
        steps {
            maven("test -Dproject.name=${project}/${branchName}")
        }
    }
}


QUEUE_JOB
Schedule a job to be executable after the DSL runs
queue(String jobName)
queue(Job job)



DISABLE_JOB
job("MyJob1"){
  disabled(true);
}



DELETE_JOB
To delete a job, you have to set the "Action for removed jobs" option to "Delete"
in the "Process Job DSLs" build step configuration.
Then remove the job from your script and run the seed job.

job('seed_all') {
  steps {
    dsl {
      external('*_jobdsl.groovy')
      // default behavior is: removeAction('IGNORE')
      removeAction('DELETE')
    }
  }
}

Now, run "seed_all" job to apply your change ("seed_all" overwrites its own configuration when run).



LIST_FILES_IN_WORKSPACE
hudson.FilePath workspace = hudson.model.Executor.currentExecutor().getCurrentWorkspace()



JOB_DSL_IN_PIPELINE
node {
    jobDsl scriptText: 'job("example-2")'

    jobDsl targets: ['jobs/projectA/*.groovy', 'jobs/common.groovy'].join('\n'),
           removedJobAction: 'DELETE',
           removedViewAction: 'DELETE',
           lookupStrategy: 'SEED_JOB',
           additionalClasspath: ['libA.jar', 'libB.jar'].join('\n')
}




FOLDERS
folder(String name, Closure closure = null) // since 1.30

Items can be created within folders by using the full path as job name.

folder('project-a')

freeStyleJob('project-a/compile')

listView('project-a/pipeline')

folder('project-a/testing')


UPLOAD_ARBITRARY_FILES
Any content can be upload from the seed job's workspace.
userContent('acme.png', streamFileFromWorkspace('images/acme.png'))

VIEWS

To create views, the DSL provides the following methods:

listView(String name, Closure closure = null)             // since 1.30
sectionedView(String name, Closure closure = null)        // since 1.30
nestedView(String name, Closure closure = null)           // since 1.30
deliveryPipelineView(String name, Closure closure = null) // since 1.30
buildPipelineView(String name, Closure closure = null)    // since 1.30
buildMonitorView(String name, Closure closure = null)     // since 1.30
categorizedJobsView(String name, Closure closure = null)  // since 1.31


MAPPING_UI_WITH_JOBDSL
Job type 	job() workflowJob() 
Build params	parameters
SCM	 	scm
Build trigg	triggers
Steps		steps
Post Build Act	publishers
