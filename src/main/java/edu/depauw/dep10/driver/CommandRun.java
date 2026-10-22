package edu.depauw.dep10.driver;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class CommandRun {
	@Parameter(description = "object")
	public List<String> parameters = new ArrayList<>();

	@Parameter(names = { "-h", "--help" }, description = "Display this help message and exit.", help = true, order = 0)
	public boolean showHelp = false;

	@Parameter(names = "--os", description = "Name of OS.", order = 1)
	public String osName = null;

	@Parameter(names = "--bm", description = "Use bare metal OS.", order = 2)
	public boolean bareMetal = false;
	
	@Parameter(names = {"-i", "--charIn"}, description = "Console input.", order = 3)
	public String consoleIn = null;
	
	@Parameter(names = {"-o", "--charOut"}, description = "Console output.", order = 4)
	public String consoleOut = null;
	
	@Parameter(names = {"-e", "--errOut"}, description = "Error output.", order = 5)
	public String errOut = null;
	
	@Parameter(names = {"-d", "--mem-dump"}, description = "Name of memory dump file.", order = 6)
	public String memDump = null;
	
	@Parameter(names = {"-t", "--trace"}, description = "Tracing output.", order = 7)
	public String trace = null;
	
	@Parameter(names = "--fancy", description = "Fancy machine: supervisor/user modes, timer at 0xFFF5, invalid-instruction trap.", order = 9)
	public boolean fancy = false;

	@Parameter(names = {"-m", "--max"}, description = "Maximum number of instructions (0 for unlimited).", order = 8)
	public int max = 125000;
}
