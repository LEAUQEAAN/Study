JDK版本1.8

文件说明：
	1.Main.java为源码
	2.MIPSsim.jar为打包好的jar
	3.sample.txt输入的样例
	4.disassembly.txt反汇编的输出结果
	5.simulation.txt是汇编语言执行过程的模拟结果
	
注意：程序执行过程中会有一些输出，输出内容与文件disassembly.txt和simulation.txt里的内容一致。

Linux下：
	1.进入命令行
	2.创建目录：sudo mkdir mips
	3.进入目录：cd mips
	4.把文件MIPSsim.jar和你的sample.txt复制到mips目录
	5.执行 java -jar MIPSsim.jar  inputfilename outputfilename 
	6.结果在mips目录的disassembly.txt和outputfilename 

windows下：
	1.在C盘创建目录【mips】
	2.进入创建的目录中，把文件MIPSsim.jar和你的sample.txt复制到【mips】中 
	3.打开命令行进入【mips】目录中
	4.执行 java -jar MIPSsim.jar  inputfilename outputfilename 
	5.结果在mips目录的disassembly.txt和outputfilename 
	
注意：如果执行java -jar MIPSsim.jar没有带参数：参数默认为sample.txt  simulation.txt

