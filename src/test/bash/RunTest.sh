#!/bin/bash
## @Before Run funz daemon to launch 'runshell' calculations

failed="0"

function testRunParseError {
    $FUNZ_HOME/Funz.sh Run abcdef > testRunParseError.out 2>&1
    ok=`grep "Unknown option: abcdef" testRunParseError.out | wc -l`
    if (($ok > 0)); then rm testRunParseError.out; echo "OK";return 0;  else echo "FAILED:"; cat testRunParseError.out; return 1; fi
}

function testRun1 {
    $FUNZ_HOME/Funz.sh Run -m $CODE -if $TMP_IN -iv x1=.5 x2=0.3 -v $VERBOSITY -ad tmp/testRun1.sh > testRun1.out 2>&1
    ok=`grep "done" testRun1.out | wc -l`
    if (($ok > 0)); then rm testRun1.out; echo "OK";return 0;  else echo "FAILED:"; cat testRun1.out; return 1; fi
}


function testOutputExpression {
    $FUNZ_HOME/Funz.sh Run -m $CODE -if $TMP_IN -iv x1=.5 x2=0.3 -v $VERBOSITY -ad tmp/testOutputExpression.sh -oe 1+cat > testOutputExpression.out 2>&1
    ok=`grep "6.154316" testOutputExpression.out | wc -l`
    if (($ok > 0)); then rm testOutputExpression.out; echo "OK";return 0;  else echo "FAILED:"; cat testOutputExpression.out; return 1; fi
}

function testRun9 {
    $FUNZ_HOME/Funz.sh Run -m $CODE -if $TMP_IN -all -iv x1=.5,.6,.7 x2=0.3,.4,.5 -v $VERBOSITY -ad tmp/testRun9.sh > testRun9.out 2>&1
    ok=`grep "done" testRun9.out | wc -l`
    if (($ok > 9)); then rm testRun9.out; echo "OK"; return 0; else echo "FAILED:"; cat testRun9.out; return 1; fi
}

function testRun9all {
    $FUNZ_HOME/Funz.sh Run -m $CODE -if $TMP_IN -iv x1=.5,.6,.7 x2=0.3,.4,.5 -v $VERBOSITY -ad tmp/testRun9all.sh -all > testRun9all.out 2>&1
    ok=`grep "done" testRun9all.out | wc -l`
    if (($ok > 9)); then rm testRun9all.out; echo "OK"; return 0; else echo "FAILED:"; cat testRun9all.out; return 1; fi
}

FUNZ_HOME="dist"

source src/test/RunTest.prop

TMP_IN=tmp/branin.R
mkdir tmp
cp $SRC $TMP_IN

for t in testRunParseError testRun1 testOutputExpression testRun9 testRun9all; do
    res=`$t`
    if [ $? = "1" ]; then failed="1"; fi
    echo "Test "$t": "$res
done

exit $failed
