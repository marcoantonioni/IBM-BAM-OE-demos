package marco.demos.kogito;

import java.math.BigDecimal;

public class ConsumeCPU {
    public static String wasteCpu(BigDecimal loops, BigDecimal innerLoops) {        
        int _loops = loops.intValue();
        int _innerLoops = innerLoops.intValue();
        if (_loops < 1) _loops = 1;
        if (_innerLoops < 1) _innerLoops = 1;

        // System.out.println("===> wasteCpu loops["+_loops+"] innerLoops["+_innerLoops+"]");        

        for (int i = 0; i < _loops; i++) {
            for (int j = 0; j < _innerLoops; j++) {
                Math.pow(1234.1234, 2345.2345);    
            }            
        }        
        return "done";
    }

    public static String wasteCpuForTime(BigDecimal milliseconds) {
        int _milliseconds = milliseconds.intValue();
        if (_milliseconds < 1) _milliseconds = 1;

        System.out.println("===> wasteCpuForTime milliseconds: "+_milliseconds);        

        boolean stop = false;
        long _startTime = System.currentTimeMillis();
        do {
            Math.pow(1234.1234, 2345.2345);    
            long _currentTime = System.currentTimeMillis();
            stop = (_currentTime - _startTime) >= _milliseconds;
        } while(! stop);

        return "done";
    }
}
