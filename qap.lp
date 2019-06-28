\ENCODING=ISO-8859-1
\Problem name: ilog.cplex

Minimize
 obj: 1027.348 x1 + 12.966 x2 + 1012.987 x3 + 27.843 x4
Subject To
 ctSuccessor_0#0:                x2 + x5  = 1
 ctSuccessor_2#1:                x4  = 1
 ctPredeccor_0#2:                x1  = 1
 ctPredeccor_2#3:                x3 + x5  = 1
 ctDistCal_m1_0#4:               - 228.753 x1 + dist_0 >= -200
 ctDistCal_m1_2#5:               - 223.896 x3 + dist_2 >= -200
 ctDistCal_0_2#6:                - 219.567 x5 - dist_0 + dist_2 >= -200
 ctMaxDistRecharge_0_2147483647#7: 4.322 x2 + dist_0
                                 - RgctMaxDistRecharge_0_2147483647#7  = 0
 ctMaxDistRecharge_2_2147483647#8: 9.281 x4 + dist_2
                                 - RgctMaxDistRecharge_2_2147483647#8  = 0
Bounds
 0 <= x1 <= 1
 0 <= x2 <= 1
 0 <= x3 <= 1
 0 <= x4 <= 1
 0 <= x5 <= 1
 0 <= RgctMaxDistRecharge_0_2147483647#7 <= 100
 0 <= RgctMaxDistRecharge_2_2147483647#8 <= 100
Generals
 x1  x2  x3  x4  x5 
End
