package com.astesbas.z80.hacker.engine;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.astesbas.z80.hacker.domain.Memory;
import com.astesbas.z80.hacker.domain.OpCode;
import com.astesbas.z80.hacker.domain.OpCodePrefix;
import com.astesbas.z80.hacker.util.SystemOut;

/**
 * Z80 Disassembler.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * 
 * @since 02/jun/2017
 */
public class Z80Disassembler {
    
    /**
     * Using a map to group the Z80 opcodes into four main groups:
     *     Opcodes starting with "CB" byte (bit and rotation ops)
     *     Opcodes starting with "DD" byte (almost all IX reg related ops)
     *     Opcodes starting with "FD" byte (almost all IY reg related ops)
     *     All other opcodes that does not fall in the groups above
    */
    private final Map<OpCodePrefix, List<OpCode>> opCodesMap = new HashMap<>();
    
    /** List containing the start points (address) in memory to be disassembled */
    private final List<Integer> startOffList = new  ArrayList<>();
    
    /** Mapping for labels entries at given addresses */
    private final Map<Integer, String> labelsMap = new HashMap<>();
    
    /** Mapping of EQU entries */
    private final Map<String, String> equsMap = new HashMap<>();
    
    /** The default output writer */
    private BufferedWriter defaultWriter = new BufferedWriter(new PrintWriter(System.out));
    
    /** The output (.asm) file path */
    private Path outputPath = Paths.get("./output.asm");
    
    /** The list (.lst) output file path */
    private Path listPath = Paths.get("./output.lst");
    
    /** The log (.log) file path */
    private Path logPath = Paths.get("./output.log");
    
    /** The memory to be disassembled (binary data) */
    private Memory memory = new Memory();
    
    // disassembler formatting parameters
    private int dbAlign = 16;
    private int tabSize = 4;
    
    // disassembler process flags and parameters
    private boolean autoDjnzLabels = true;
    private boolean autoJrLabels = true;
    private boolean autoJpLabels = true;
    private boolean autoCallLabels = true;
    private int lowMem = Memory.START_ADDRESS;
    private int highMem = Memory.END_ADDRESS;
    
//    private Charset charset = Charset.forName("UTF-8");
    
    /**
     * Z80 Disassembler constructor.
     */
    public Z80Disassembler() {
        
        // initializes the op-codes map by prefix
        for(OpCodePrefix prefix:OpCodePrefix.values()) {
            this.opCodesMap.put(prefix, new ArrayList<>());
        }   
    }   
    
    /**
     * Maps a label to be used as reference in the disassembled code.
     * @param label the key label
     * @param address the address to be mapped to the given label
     */
    public void mapLabel(String label, Integer address) {
        this.labelsMap.put(address, label);
    }   
    
    /**
     * Maps an EQU directive.
     * @param label the key label
     * @param value the value to be mapped to the given equ label
     */
    public void mapEqu(String label, String value) {
        this.equsMap.put(label, value);
    }   
    
    /**
     * Sets the output file name (disassembled code)
     * @param outputFileName the output file name
     */
    public void setOutputFile(String outputFileName) {
        this.outputPath = this.getFilePath(outputFileName);
    }   
    
    /**
     * Sets the list file path.
     * @param listFileName the listFile name
     */
    public void setListFile(String listFileName) {
        this.listPath = this.getFilePath(listFileName);
    }   
    
    /**
     * Sets the log file path.
     * @param logFileName the logFile name to set
     */
    public void setLogFile(String logFileName) {
        this.logPath = this.getFilePath(logFileName);
    }   
    
    /**
     * Get the Path for the given file name.
     * @param fileName the file
     * @return the path for the file name
     */
    public Path getFilePath(String fileName) {
        Path path = null;
        try {
            path = Paths.get(fileName);
            Files.deleteIfExists(path);
        } catch (IOException | InvalidPathException exception) {
            System.err.printf("%nError getting path to file: \"%s\"\n\t%s%n", fileName, exception.getMessage());
            System.exit(-1);
        }   
        return path;
    }   
    
    /**
     * Sets the memory to be disassembled.<br/>
     * The memory reference is stored instead of copying the original memory to an internal object.
     * This is not a problem since the memory data will not be changed.
     * 
     * @param memory the memory to set
     */
    public void setMemoryData(Memory memory) {
        assert memory != null;
        this.memory = memory;
    }      
    
    /**
     * @param dbAlign the dbAlign to set
     */
    public void setDbAlign(int dbAlign) {
        this.dbAlign = dbAlign;
    }

    /**
     * @param tabSize the tabSize to set
     */
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    /**
     * @param autoDjnzLabels the autoDjnzLabels to set
     */
    public void setAutoDjnzLabels(boolean autoDjnzLabels) {
        this.autoDjnzLabels = autoDjnzLabels;
    }

    /**
     * @param autoJrLabels the autoJrLabels to set
     */
    public void setAutoJrLabels(boolean autoJrLabels) {
        this.autoJrLabels = autoJrLabels;
    }

    /**
     * @param autoJpLabels the autoJpLabels to set
     */
    public void setAutoJpLabels(boolean autoJpLabels) {
        this.autoJpLabels = autoJpLabels;
    }

    /**
     * @param autoCallLabels the autoCallLabels to set
     */
    public void setAutoCallLabels(boolean autoCallLabels) {
        this.autoCallLabels = autoCallLabels;
    }

    /**
     * @param lowMem the lowMem to set
     */
    public void setLowMem(int lowMem) {
        this.lowMem = lowMem;
    }

    /**
     * @param highMem the highMem to set
     */
    public void setHighMem(int highMem) {
        this.highMem = highMem;
    }
    
    /**
     * Pushes a starting address point for disassembler process.
     * @param address the start address for disassembling
     */
    public void pushStartAddress(Integer address) {
        this.startOffList.add(address);
    }   
    
    /**
     * Return the next start address point for disassembling.
     * @return the start address for disassembling
     */
    private Integer popStartAddress() {
        return this.startOffList.isEmpty() ? null:this.startOffList.remove(0);
    }   
    
    /**
     * Output text to log file.
     * @param format the string formatter
     * @param args the parameters for the log formatter
     */
    private void log(String format, Object... args) {
        
        // append log file for writing (create a new file if necessary)
        try (BufferedWriter writer = Files.newBufferedWriter(this.logPath, CREATE, APPEND)) {
            String text = String.format(format, args);
            writer.write(text, 0, text.length());
            SystemOut.vprint("[LOG] "+text);
        } catch (IOException ioException) {
            System.err.format("IOException: %s%n", ioException);
            System.exit(-1);
        }   
    }   
    
    /**
     * @throws IOException 
     * 
     */
    public void exec() {
        
        this.log("Starting disassembler process at %s%n", new Date(System.currentTimeMillis()));
        this.log("Output file: %s%n", this.outputPath);
        
        // keep running until the start-off list is empty 
        while(!this.startOffList.isEmpty()) {
            
            // set the memory pointer equals to the start address
            Integer startAddress = this.popStartAddress();
            this.memory.setPointer(startAddress);
            this.log("Processing start-off address: 0x%X%n", startAddress);
            
            while(true) {
                
                OpCodePrefix prefix = OpCodePrefix.of(this.memory.get());
                
                List<OpCode> opCodes = this.opCodesMap.get(prefix);
                
                break;
//                switch(OpCodePrefix.of(byteData)) {
//                    
//                    case NONE:
//                        System.out.println();
//                        break;
//                        
//                    default:
//                        break;
//                }
            }
            
        }
        
    }
    
    private OpCode getOpCode(byte[] bytes) {
        return null;
    }
    
    /**
     * Get input stream resource for given file name.
     * @param fileName name of the resource
     * @return input stream resource
     */
    public InputStream getResourceAsStream(String fileName) {
        return this.getClass().getResourceAsStream(fileName);
    }   
    
    /**
     * Loads the op codes data from file (patterns and attributes).
     * 
     * @param file
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    public synchronized void loadOpCodesFromFile(String fileName)
            throws IOException, IllegalArgumentException {
        InputStream stream = this.getResourceAsStream(fileName);
    	this.loadOpCodesFromStream(stream);
    }   
    
    /**
     * Loads the op codes data from input stream (patterns and attributes).
     * 
     * @param iputStream the input stream
     * @throws IOException if some reading error occurs
     * @throws IllegalArgumentException if the input file has some invalid data
     */
    public synchronized void loadOpCodesFromStream(InputStream iputStream) 
            throws IOException, IllegalArgumentException {
        
        // line data read from text file
        String line;
        
        // the current line number (used for error messages)
        int lineNumber = 0;
        int instructionsCounter = 0;
        
        // InputStreamReader reads bytes and decodes them into characters using a specified charset
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(iputStream))) {
            
            SystemOut.vprint("Loading op-codes from resource file...");
            
            while ((line = bufferedReader.readLine()) != null) {
                
                // update the line number counter
                lineNumber++;
                
                // discard comment lines and empty lines
                line = line.replace('\t', ' ').trim();
                if (line.isEmpty() || line.startsWith(";")) {
                    continue;
                }   
                
                // get the opcode and the mnemonic string format 
                String lineSplit[] = line.split(":");
                if (lineSplit.length > 1) {
                    
                    // get the op-code/mnemonic string representations
                    String opCodeString = lineSplit[0].trim();
                    String mnemonicString = lineSplit[1].trim();
                    
                    // create the OpCode for the instruction and get the group prefix
                    OpCode opCode = new OpCode(opCodeString, mnemonicString);
                    
                    // add the opcode to the corresponding op-code list (mapped by prefix)
                    this.opCodesMap.get(opCode.getPrefix()).add(opCode);
                    
                    // update the processed instruction counter
                    instructionsCounter++;
                    
                } else {
                    SystemOut.vprintln("Error!");
                    throw new IllegalArgumentException(
                        String.format("Error processing instruction \"%s\" at line %d%n", line, lineNumber)
                    );
                }   
            }   
            
            SystemOut.vprintln("Ok");
            SystemOut.vprintf("Total of instructions read: %d\n", instructionsCounter);
            
        } catch(NullPointerException ioException) {
            //  a NullPointerException may occur if the op-codes.dat cannot be found in the classpath
            throw new IOException("System could not find the op-codes resource file in the classpath!");
        }
    }   
	
}