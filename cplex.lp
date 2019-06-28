\ENCODING=ISO-8859-1
\Problem name: ilog.cplex

Minimize
 obj: 1099.225 block_1 + 1099.531 block_2 + 1172.803 block_3 + 55 duty_1
      + 395 duty_2 + 340 duty_3 + 345 duty_4 + 135 duty_5 + 330 duty_6
      + 335 duty_7 + 400 duty_8 + 345 duty_9 + 350 duty_10 + 335 duty_11
      + 340 duty_12 + 675 duty_13 + 680 duty_14 + 680 duty_15 + 685 duty_16
Subject To
 ctTripVehicle_0:          block_1 + block_3  = 1
 ctTripVehicle_2:          block_2 + block_3  = 1
 ctTripDriver_0:           duty_6 + duty_7 + duty_11 + duty_12 + duty_13
                           + duty_14 + duty_15 + duty_16 >= 1
 ctTripDriver_2:           duty_2 + duty_3 + duty_4 + duty_8 + duty_9 + duty_10
                           + duty_13 + duty_14 + duty_15 + duty_16 >= 1
 ctDeadrunLowerLimit_4:    - block_2 - block_3 + duty_5 + duty_8 + duty_9
                           + duty_10 + duty_15 + duty_16 >= 0
 ctDeadrunLowerLimit_1:    - block_1 - block_3 + duty_7 + duty_12 + duty_14
                           + duty_16 >= 0
 ctDeadrunLowerLimit_3:    - block_2 + duty_4 + duty_10 >= 0
 ctDeadrunLowerLimit_2:    - block_1 + duty_1 + duty_11 + duty_12 >= 0
 ctDeadrunUpperLimit_4:    - 2 block_2 - 2 block_3 + duty_5 + duty_8 + duty_9
                           + duty_10 + duty_15 + duty_16 <= 0
 ctDeadrunUpperLimit_1:    - 2 block_1 - 2 block_3 + duty_7 + duty_12 + duty_14
                           + duty_16 <= 0
 ctDeadrunUpperLimit_3:    - 2 block_2 + duty_4 + duty_10 <= 0
 ctDeadrunUpperLimit_2:    - 2 block_1 + duty_1 + duty_11 + duty_12 <= 0
 ctIdleTime_224_1393_1404: - block_3 + duty_2 + duty_8 + duty_13 + duty_14
                           + duty_15 + duty_16  = 0
Bounds
 0 <= block_1 <= 1
 0 <= block_2 <= 1
 0 <= block_3 <= 1
 0 <= duty_1 <= 1
 0 <= duty_2 <= 1
 0 <= duty_3 <= 1
 0 <= duty_4 <= 1
 0 <= duty_5 <= 1
 0 <= duty_6 <= 1
 0 <= duty_7 <= 1
 0 <= duty_8 <= 1
 0 <= duty_9 <= 1
 0 <= duty_10 <= 1
 0 <= duty_11 <= 1
 0 <= duty_12 <= 1
 0 <= duty_13 <= 1
 0 <= duty_14 <= 1
 0 <= duty_15 <= 1
 0 <= duty_16 <= 1
Generals
 block_1  block_2  block_3  duty_1  duty_2  duty_3  duty_4  duty_5  duty_6 
 duty_7  duty_8  duty_9  duty_10  duty_11  duty_12  duty_13  duty_14  duty_15 
 duty_16 
End
