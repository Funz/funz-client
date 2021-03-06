cat <<EOF > run.R
FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME)
res=Funz_Run("R","src/test/samples/branin.R",input.variables = list(x1=(1:9)/10,x2=(1:9)/10),run.control=list(blacklistTimeout=1),verbosity=5)
print(unlist(res[['cat']]))
q(save="no")
EOF

rm Run.log
rm run.Rout
R CMD BATCH run.R 2>&1 > run.Rout &
PID_R=$!
echo "PID_R = "$PID_R

sleep 1 # let coming 'ps' update 

ok1=`ps aux | tee ps.out | grep " $PID_R " | grep "\.R" | wc -l`
if [ ! $ok1 = "1" ]; then echo "FAILED to start client: $ok1"; echo "* ps.out"; cat ps.out; echo "* run.Rout"; cat run.Rout; echo "* Run.log"; cat Run.log; exit 1; fi
echo "OK started client"

rm calc.out
FUNZ_HOME="../funz-calculator/dist"
LIB=`find $FUNZ_HOME/lib -name "funz-core-*.jar"`:`find $FUNZ_HOME/lib -name "funz-calculator-*.jar"`:`find $FUNZ_HOME/lib -name "commons-io-*.jar"`:`find $FUNZ_HOME/lib -name "commons-exec-*.jar"`:`find $FUNZ_HOME/lib -name "commons-lang-*.jar"`:`find $FUNZ_HOME/lib -name "ftpserver-core-*.jar"`:`find $FUNZ_HOME/lib -name "ftplet-api-*.jar"`:`find $FUNZ_HOME/lib -name "mina-core-*.jar"`:`find $FUNZ_HOME/lib -name "sigar-*.jar"`:`find $FUNZ_HOME/lib -name "slf4j-api-*.jar"`:`find $FUNZ_HOME/lib -name "slf4j-log4j*.jar"`
java -Dapp.home=$FUNZ_HOME -classpath $LIB org.funz.calculator.Calculator file:dist/calculator.xml 2>&1 > calc.out &
PID_CALCULATOR=$!
echo "PID_CALCULATOR = "$PID_CALCULATOR

sleep 2

ok2=`ps aux | tee ps.out | grep " $PID_CALCULATOR " | grep java | wc -l`
if [ ! $ok2 = "1" ]; then echo "FAILED to start calculator: $ok2"; echo "* ps.out"; cat ps.out; kill -9 $PID_R; echo "* calc.out"; cat calc.out; exit 2; fi
echo "OK started calculator"

## for loop testing of previous Run only. Comment otherwise
# wait $PID_R
# kill -9 $PID_CALCULATOR

sleep 10

echo "///////////////////////////////// KILL CALCULATOR /////////////////////////////////////" >> run.Rout
echo "" >> run.Rout
sleep 1
kill -9 $PID_CALCULATOR

sleep 3

ok3=`ps aux | tee ps.out | grep " $PID_CALCULATOR " | grep java | wc -l`
if [ ! $ok3 = "0" ]; then echo "FAILED to stop calculation: $ok3"; echo "* ps.out"; cat ps.out; kill -9 $PID_R $PID_CALCULATOR; cat calc.out; exit 3; fi
echo "OK to stop calculation"

ok4=`ps aux | tee ps.out | grep " $PID_R " | grep "\.R" | wc -l`
if [ ! $ok4 = "1" ]; then echo "FAILED to let client alive: $ok4"; echo "* ps.out"; cat ps.out; kill -9 $PID_R $PID_CALCULATOR; echo "* run.Rout"; cat run.Rout; echo "* Run.log"; cat Run.log; exit 4; fi
echo "OK client alive"

rm calc.out
java -Dapp.home=$FUNZ_HOME -classpath $LIB org.funz.calculator.Calculator file:dist/calculator.xml 2>&1 > calc1.out &
PID_CALCULATOR1=$!
echo "PID_CALCULATOR1 = "$PID_CALCULATOR1
java -Dapp.home=$FUNZ_HOME -classpath $LIB org.funz.calculator.Calculator file:dist/calculator.xml 2>&1 > calc2.out &
PID_CALCULATOR2=$!
echo "PID_CALCULATOR2 = "$PID_CALCULATOR2

sleep 3

ok5=`ps aux | tee ps.out | grep " $PID_R " | grep "\.R" | wc -l`
if [ ! $ok5 = "1" ]; then echo "FAILED to restart calculation: $ok5"; echo "* ps.out"; cat ps.out; kill -9 $PID_R $PID_CALCULATOR; cat run.Rout; exit 5; fi
echo "OK to restart calculation"

done=0
i=0
while [ $i -le 10 ]; do
  ok_6=`ps aux | grep " $PID_R " | grep "\.R" | wc -l`
  if [ $ok_6 = "0" ]; then
      break
  fi
  i=$(( $i + 1 ))
  sleep 10
done
echo "Waited "$i"x10s"

ok6=`ps aux | tee ps.out | grep " $PID_R " | grep "\.R" | wc -l`
if [ ! $ok6 = "0" ]; then echo "FAILED to finish calculation: $ok6"; echo "* ps.out"; cat ps.out; kill -9 $PID_R $PID_CALCULATOR; cat run.Rout; exit 6; fi
echo "OK to finish calculation"

ok7=`tail -10 run.Rout | grep "136.0767" | wc -l`
if [ ! $ok7 = "1" ]; then echo "FAILED to complete calculation: $ok7"; kill -9 $PID_R $PID_CALCULATOR; cat run.Rout; exit 7; fi
echo "OK to complete calculation"

kill -9 $PID_CALCULATOR1 $PID_CALCULATOR2
