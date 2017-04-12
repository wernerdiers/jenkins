//always-running step at the end.
try {
         sh "false"
    } finally {

        stage 'finalize'
        echo "I will always run!"
    }

//You can add a stage for post build to add post build action in pipeline:
try {
    //Stages to be included in build
    ...
} catch {
    ...
} finally {
    stage 'post-build'
    ...
}


//Run only if build succeeds

stage 'build'
... build
... tests
stage 'post-build'
...

//Run only if build succeeds or is unstable

stage 'build'
... build
try {
    ... tests
} catch {
    ...
}
stage 'post-build'

//Run regardless of build result - could it be done using try / catch / finally ?

try {
    stage 'build'
    ...
} catch {
    ...
} finally {
    stage 'post-build'
    ...
}

/*
I've noticed that final build status is set as SUCCESS even though some stages,
ie. 'build', have failed as it set based on last stage. Does that mean final
build status need to explicitly set, ie.currentBuild.result = 'UNSTABLE'? 

If you are using try/catch and you want a build to be marked as unstable or
failed then you must use currentBuild.result = 'UNSTABLE' etc. I believe some 
plugins like the JUnit Report plugin will set this for you if it finds failed 
tests in the junit results. But in most cases you have to set it your self if
you are catching errors.

 */


 //The second option if you don't want to continue is to rethrow the error.

stage 'build'
... build
try {
    ... tests
} catch(err) {
    //do something then re-throw error if needed.
    throw(err)
}
stage 'post-build'
...
