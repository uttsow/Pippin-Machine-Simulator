CMPZ 0
SUB #1
JMPZ #F
CMPL 0
SUB #1
JMPZ #E
LOD 0
STO 1
LOD 0 
SUB #1
STO 0
CMPZ 0
SUB #1
JMPZ #6
LOD 0
MUL 1
DIV 10	
NOT
STO 1
HALT
DATA
0 8
