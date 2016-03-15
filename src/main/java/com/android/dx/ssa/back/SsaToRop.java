/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.ssa.back;

import com.android.dx.rop.code.BasicBlock;
import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.InsnList;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.Rops;
import com.android.dx.ssa.BasicRegisterMapper;
import com.android.dx.ssa.PhiInsn;
import com.android.dx.ssa.RegisterMapper;
import com.android.dx.ssa.SsaBasicBlock;
import com.android.dx.ssa.SsaInsn;
import com.android.dx.ssa.SsaMethod;
import com.android.dx.util.Hex;
import com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * Converts a method in SSA form to ROP form.
 */
public class SsaToRop {
    /** local debug flag */
    private static final boolean DEBUG = false;

    /** {@code non-null;} method to process */
    private final SsaMethod ssaMeth;

    /** {@code non-null;} interference graph */
    private final InterferenceGraph interference;

    /**
     * Converts a method in SSA form to ROP form.
     *
     * @param ssaMeth {@code non-null;} method to process
     * @return {@code non-null;} rop-form output
     */
    public static RopMethod convertToRopMethod(SsaMethod ssaMeth) {
        return new SsaToRop(ssaMeth).convert();
    }

    /**
     * Constructs an instance.
     *  @param ssaMeth {@code non-null;} method to process
     *
     */
    private SsaToRop(SsaMethod ssaMethod) {
        this.ssaMeth = ssaMethod;
        this.interference =
            LivenessAnalyzer.constructInterferenceGraph(ssaMethod);
    }

    /**
     * Performs the conversion.
     *
     * @return {@code non-null;} rop-form output
     */
    private RopMethod convert() {
        if (DEBUG) {
            interference.dumpToStdout();
        }

        // These are other allocators for debugging or historical comparison:
        // allocator = new NullRegisterAllocator(ssaMeth, interference);
        // allocator = new FirstFitAllocator(ssaMeth, interference);

        RegisterAllocator allocator =
            new FirstFitLocalCombiningAllocator(ssaMeth, interference
            );

        RegisterMapper mapper = allocator.allocateRegisters();

        if (DEBUG) {
            System.out.println("Printing reg map");
            System.out.println(((BasicRegisterMapper)mapper).toHuman());
        }

        ssaMeth.setBackMode();

        ssaMeth.mapRegisters(mapper);

        removePhiFunctions();

        if (allocator.wantsParamsMovedHigh()) {
            moveParametersToHighRegisters();
        }

        removeEmptyGotos();

        RopMethod ropMethod = new RopMethod(convertBasicBlocks(),
                ssaMeth.blockIndexToRopLabel(ssaMeth.getEntryBlockIndex()));
        ropMethod = new IdenticalBlockCombiner(ropMethod).process();

        return ropMethod;
    }

    /**
     * Removes all blocks containing only GOTOs from the control flow.
     * Although much of this work will be done later when converting
     * from rop to dex, not all simplification cases can be handled
     * there. Furthermore, any no-op block between the exit block and
     * blocks containing the real return or throw statements must be
     * removed.
     */
    private void removeEmptyGotos() {
        final ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();

        ssaMeth.forEachBlockDepthFirst(false, new SsaBasicBlock.Visitor() {
            public void visitBlock(SsaBasicBlock b, SsaBasicBlock parent) {
                ArrayList<SsaInsn> insns = b.getInsns();

                if ((insns.size() == 1)
                        && (insns.get(0).getOpcode() == Rops.GOTO)) {
                    BitSet preds = (BitSet) b.getPredecessors().clone();

                    for (int i = preds.nextSetBit(0); i >= 0;
                            i = preds.nextSetBit(i + 1)) {
                        SsaBasicBlock pb = blocks.get(i);
                        pb.replaceSuccessor(b.getIndex(),
                                b.getPrimarySuccessorIndex());
                    }
                }
            }
        });
    }

    /**
     * See Appel 19.6. To remove the phi instructions in an edge-split
     * SSA representation we know we can always insert a move in a
     * predecessor block.
     */
    private void removePhiFunctions() {
        ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();

        for (SsaBasicBlock block : blocks) {
            // Add moves in all the pred blocks for each phi insn.
            block.forEachPhiInsn(new PhiVisitor(blocks));

            // Delete the phi insns.
            block.removeAllPhiInsns();
        }

        /*
         * After all move insns have been added, sort them so they don't
         * destructively interfere.
         */
        for (SsaBasicBlock block : blocks) {
            block.scheduleMovesFromPhis();
        }
    }

    /**
     * Helper for {@link #removePhiFunctions}: PhiSuccessorUpdater for
     * adding move instructions to predecessors based on phi insns.
     */
    private static class PhiVisitor implements PhiInsn.Visitor {
        private final ArrayList<SsaBasicBlock> blocks;

        public PhiVisitor(ArrayList<SsaBasicBlock> blocks) {
            this.blocks = blocks;
        }

        public void visitPhiInsn(PhiInsn insn) {
            RegisterSpecList sources = insn.getSources();
            RegisterSpec result = insn.getResult();
            int sz = sources.size();

            for (int i = 0; i < sz; i++) {
                RegisterSpec source = sources.get(i);
                SsaBasicBlock predBlock = blocks.get(
                        insn.predBlockIndexForSourcesIndex(i));

                predBlock.addMoveToEnd(result, source);
            }
        }
    }

    /**
     * Moves the parameter registers, which allocateRegisters() places
     * at the bottom of the frame, up to the top of the frame to match
     * Dalvik calling convention.
     */
    private void moveParametersToHighRegisters() {
        int paramWidth = ssaMeth.getParamWidth();
        BasicRegisterMapper mapper
                = new BasicRegisterMapper(ssaMeth.getRegCount());
        int regCount = ssaMeth.getRegCount();

        for (int i = 0; i < regCount; i++) {
            if (i < paramWidth) {
                mapper.addMapping(i, regCount - paramWidth + i, 1);
            } else {
                mapper.addMapping(i, i - paramWidth, 1);
            }
        }

        if (DEBUG) {
            System.out.printf("Moving %d registers from 0 to %d\n",
                    paramWidth, regCount - paramWidth);
        }

        ssaMeth.mapRegisters(mapper);
    }

    /**
     * @return rop-form basic block list
     */
    private BasicBlockList convertBasicBlocks() {
        ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();

        // Exit block may be null.
        SsaBasicBlock exitBlock = ssaMeth.getExitBlock();

        ssaMeth.computeReachability();
        int ropBlockCount = ssaMeth.getCountReachableBlocks();

        // Don't count the exit block, if it exists and is reachable.
        ropBlockCount -= (exitBlock != null && exitBlock.isReachable()) ? 1 : 0;

        BasicBlockList result = new BasicBlockList(ropBlockCount);

        // Convert all the reachable blocks except the exit block.
        int ropBlockIndex = 0;
        for (SsaBasicBlock b : blocks) {
            if (b.isReachable() && b != exitBlock) {
                result.set(ropBlockIndex++, convertBasicBlock(b));
            }
        }

        // The exit block, which is discarded, must do nothing.
        if (exitBlock != null && exitBlock.getInsns().size() != 0) {
            throw new RuntimeException(
                    "Exit block must have no insns when leaving SSA form");
        }

        return result;
    }

    /**
     * Validates that a basic block is a valid end predecessor. It must
     * end in a RETURN or a THROW. Throws a runtime exception on error.
     *
     * @param b {@code non-null;} block to validate
     * @throws RuntimeException on error
     */
    private void verifyValidExitPredecessor(SsaBasicBlock b) {
        ArrayList<SsaInsn> insns = b.getInsns();
        SsaInsn lastInsn = insns.get(insns.size() - 1);
        Rop opcode = lastInsn.getOpcode();

        if (opcode.getBranchingness() != Rop.BRANCH_RETURN
                && opcode != Rops.THROW) {
            throw new RuntimeException("Exit predecessor must end"
                    + " in valid exit statement.");
        }
    }

    /**
     * Converts a single basic block to rop form.
     *
     * @param block SSA block to process
     * @return {@code non-null;} ROP block
     */
    private BasicBlock convertBasicBlock(SsaBasicBlock block) {
        IntList successorList = block.getRopLabelSuccessorList();
        int primarySuccessorLabel = block.getPrimarySuccessorRopLabel();

        // Filter out any reference to the SSA form's exit block.

        // Exit block may be null.
        SsaBasicBlock exitBlock = ssaMeth.getExitBlock();
        int exitRopLabel = (exitBlock == null) ? -1 : exitBlock.getRopLabel();

        if (successorList.contains(exitRopLabel)) {
            if (successorList.size() > 1) {
                throw new RuntimeException(
                        "Exit predecessor must have no other successors"
                                + Hex.u2(block.getRopLabel()));
            } else {
                successorList = IntList.EMPTY;
                primarySuccessorLabel = -1;

                verifyValidExitPredecessor(block);
            }
        }

        successorList.setImmutable();

        return new BasicBlock(
                block.getRopLabel(), convertInsns(block.getInsns()),
                successorList,
                primarySuccessorLabel);
    }

    /**
     * Converts an insn list to rop form.
     *
     * @param ssaInsns {@code non-null;} old instructions
     * @return {@code non-null;} immutable instruction list
     */
    private InsnList convertInsns(ArrayList<SsaInsn> ssaInsns) {
        int insnCount = ssaInsns.size();
        InsnList result = new InsnList(insnCount);

        for (int i = 0; i < insnCount; i++) {
            result.set(i, ssaInsns.get(i).toRopInsn());
        }

        result.setImmutable();

        return result;
    }

}
