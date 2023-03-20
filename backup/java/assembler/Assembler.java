/*
 * see license.txt
 */
package assembler;

import assembler.emulation.Memory;
import assembler.emulation.Processor;
import assembler.emulation.Register;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assembler {
    
    private final List<Integer> instructions;
    private final List<Number> numPool;
    private final List<String> strPool;
    
    private final Map<String, Integer> labels;
    private final Map<Integer, AssemblerInstruction> pendingInstructions;
    private final Map<String, Integer> constants;

    private final AssemblerParser parser;
    private final Processor processor;
    private final Memory memory;
    
    public Assembler(Processor processor, Memory memory) {
        this.processor = processor;
        this.memory = memory;
        
        this.instructions = new ArrayList<>();
        
        this.numPool = new ArrayList<>();
        this.strPool = new ArrayList<>();
        
        this.constants = new HashMap<>();
        
        this.labels = new HashMap<>();
        this.pendingInstructions = new HashMap<>();

        Map<String, Integer> registers = new HashMap<>();
        
        Register[] regs = processor.getRegisters();
        for(int index = 0; index < regs.length; index++) {
            Register register = regs[index];
            registers.put(register.getName(), index);
        }
        
        this.parser = new AssemblerParser(registers, constants);
    }
    
    
    private void addInstruction(AssemblerInstruction instr) {
        
        // if any of the arguments starts with a label marker,
        // mark this as a pending instruction, so that we can
        // reconcile all of the labels
        if(instr.args.stream().anyMatch(a -> a.startsWith(":"))) {
            addInstruction(0);
            
            this.pendingInstructions.put(this.instructions.size() - 1, instr);
        }
        else {            
            int instruction = this.parser.parseInstruction(instr);
            addInstruction(instruction);
        }
    }
    
    private void addInstruction(int instruction) {
        this.instructions.add(instruction);
    }
       
    
    private void reconcileLabels() {
        this.pendingInstructions.entrySet().forEach( entry -> {
            AssemblerInstruction instr = entry.getValue();
            List<String> args = instr.args;
            String arg1 = args.get(1);
            String arg2 = null;
            if(args.size() > 2) {
                arg2 = args.get(2);
            }
            
            if(arg1.startsWith(":")) {
                arg1 = "#" + this.labels.get(arg1);                
            }
            
            if(arg2 != null && arg2.startsWith(":")) {
                arg2 = "#" + this.labels.get(arg2);                
            }            
            
            int instruction = this.parser.parseInstruction(instr, arg1, arg2);                        
            this.instructions.set(entry.getKey(), instruction );
        });
    }
        
    private int[] buildConstants(List<AssemblerInstruction> parsedLines) {
        class ConstantEntry {
            final String constantName;
            final int index;
            final boolean isNumber;
            
            ConstantEntry(String constantName, int index, boolean isNumber) {
                this.constantName = constantName;
                this.index = index;
                this.isNumber = isNumber;
            }
        }
        
        List<ConstantEntry> constantEntries = new ArrayList<>();
        
        // Build up the constant pools
        for(AssemblerInstruction instr: parsedLines) {
            List<String> args = instr.args;
            if(!args.isEmpty()) {
                String opcode = args.get(0);
                if(opcode != null && opcode.startsWith(".")) {
                    if(args.size() < 2) {
                        throw this.parser.parseError(instr, "Illegal constant expression: '" + args + "'");                        
                    }
                    
                    int index = 0;
                    
                    String arg = args.get(1);
                    try {
                        
                        Number value = arg.contains(".") ? Float.parseFloat(arg) : Integer.parseInt(arg);
                        index = numPool.indexOf(value);
                        if(index < 0) {
                            numPool.add(value);
                            index = numPool.size();
                        }
                        
                        constantEntries.add(new ConstantEntry(opcode, index, true));
                    }
                    catch(NumberFormatException e) {
                        index = strPool.indexOf(arg);
                        if(index < 0) {
                            strPool.add(arg);
                            index = strPool.size();
                        }
                        
                        constantEntries.add(new ConstantEntry(opcode, index, false));
                    }
                    
                }
            }
        }
        
        // Build out the constant mappings (constant name => constant pool index)
        constantEntries.stream().filter(c ->  c.isNumber)
                 .forEach(c -> this.constants.put(c.constantName, c.index - 1));
        
        constantEntries.stream().filter(c -> !c.isNumber)
                 .forEach(c -> this.constants.put(c.constantName, numPool.size() + c.index - 1));
        
        
        // Now Build out the mappings to the constant pool to RAM 
        {

            int ramAddress = 0;
            final int addressInc = processor.getWordSize() / 8;
            
            int[] constants = new int[numPool.size() + strPool.size()];
            int index = 0;
            
            for(Number number : numPool) {
                if(number instanceof Float) {
                    memory.storeFloat(ramAddress, number.floatValue());
                }
                else {               
                    memory.storeInt(ramAddress, number.intValue());
                }
                
                constants[index++] = ramAddress;
                ramAddress += addressInc;
            }
            
            for(String str : strPool) {
                memory.storeStr(ramAddress, str);
                
                constants[index++] = ramAddress;
                ramAddress += (str.length() + 1); // strings are null terminated
            }
            
            // Mark the start of the Heap space
            processor.getH().address(ramAddress);
            
            return constants;        
        }
    }
    
    
    private Bytecode compileBytecode(int[] constants) {
        int[] code = new int[this.instructions.size()];
        for(int index = 0; index < code.length; index++) {
            code[index] = this.instructions.get(index);
        }
        
        return new Bytecode(constants, code, 0 , code.length);
    }


    public Bytecode compile(String assembly) {
        List<AssemblerInstruction> parsedLines = this.parser.parse(assembly);
        
        final int[] constants = buildConstants(parsedLines);
        
        for(AssemblerInstruction instr : parsedLines) {
            List<String> args = instr.args;
            String opcode = args.get(0);
            if(opcode != null && !opcode.equals("")) {
                /* The opcode can be either a Label or Data constant
                 */
                
                // Label
                if(opcode.startsWith(":")) {
                    this.labels.put(opcode, this.instructions.size());
                }
                
                // Data Constant
                else if(opcode.startsWith(".")) {
                    continue;
                }
                
                // Actual opcode instruction
                else {                    
                    addInstruction(instr);
                }
            }
        }

        reconcileLabels();
        
        return compileBytecode(constants);
    }
}
