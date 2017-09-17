package com.astesbas.z80.hacker;

import com.astesbas.z80.hacker.domain.BinaryData;
import com.astesbas.z80.hacker.engine.CmdLineArgumentsInterpreter;
import com.astesbas.z80.hacker.engine.Z80Disassembler;
import com.astesbas.z80.hacker.util.ConfigFileProperties;

/**
 * Z80 Hacker - Z80 Disassembler tool.<br/>
 * Usage:<br/>
 *     java -jar z80.disassembler-version.jar -p parametersfile
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 16/jun/2017
 */
public class Executer {
    
    /**
     * The Z80 hacker tool executer :P
     */
    public Executer() {
        System.out.println("Z80 Hacker - Version 0.1");
    }   
    
    /**
     * The main function. This is the entry point of Z80 Hacker tool!
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // the project executer
        Executer executer = new Executer();
        
        // The command line interpreter
        CmdLineArgumentsInterpreter cmdLineInterpreter = new CmdLineArgumentsInterpreter();
        cmdLineInterpreter.process(args);
        java.io.File configFile = cmdLineInterpreter.getProjectConfigFile();
        
        // Read the project configuration file
        ConfigFileProperties properties = executer.loadConfigile(configFile);
        
        // The binary data to be disassembled (in general a game ROM or a memory dump)
        BinaryData binaryData = BinaryData.fromProperties(properties);
        
        // The Z80 disassembler engine
        Z80Disassembler z80Disassembler = new Z80Disassembler(binaryData);
        z80Disassembler.setProperties(properties);
        
        // ******** execute the disassembler process
        z80Disassembler.run();
    }   
    
    /**
     * Load the project configuration from file.
     * @param configFile the configuration file name
     * @return ConfigFileProperties properties read from configuration file
     */
    private ConfigFileProperties loadConfigile(java.io.File configFile) {
        
        ConfigFileProperties configFileProperties = new ConfigFileProperties();
        
        try {
            configFileProperties.load(configFile);
        } catch (IllegalArgumentException | java.io.IOException exception) {
            System.out.printf("%nError reading configuration file %s!\n\t%s%n",
                configFile.getName(), exception.getMessage());
            System.exit(-1);
        }   
        
        return configFileProperties;
    }   
}   
