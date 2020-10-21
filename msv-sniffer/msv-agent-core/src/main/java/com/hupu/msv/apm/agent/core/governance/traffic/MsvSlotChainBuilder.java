package com.hupu.msv.apm.agent.core.governance.traffic;

import com.alibaba.csp.sentinel.slotchain.DefaultProcessorSlotChain;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotChain;
import com.alibaba.csp.sentinel.slotchain.SlotChainBuilder;
import com.alibaba.csp.sentinel.slots.block.flow.FlowSlot;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowSlot;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.alibaba.csp.sentinel.slots.nodeselector.NodeSelectorSlot;
import com.alibaba.csp.sentinel.slots.statistic.StatisticSlot;

/**
 * @author: zhaoxudong
 * @date: 2019-07-25 16:38
 * @description:
 */
public class MsvSlotChainBuilder implements SlotChainBuilder {
    /**
     * Build the processor slot chain.
     *
     * @return a processor slot that chain some slots togetherØ
     */
    @Override
    public ProcessorSlotChain build() {
        ProcessorSlotChain chain = new DefaultProcessorSlotChain();
        chain.addLast(new NodeSelectorSlot());
        chain.addLast(new ClusterBuilderSlot());
//        chain.addLast(new LogSlot());
        chain.addLast(new StatisticSlot());
        chain.addLast(new ParamFlowSlot());
//        chain.addLast(new SystemSlot());
        chain.addLast(new MsvAuthoritySlot());
        chain.addLast(new FlowSlot());
//        chain.addLast(new DegradeSlot());

        return chain;
    }
}
