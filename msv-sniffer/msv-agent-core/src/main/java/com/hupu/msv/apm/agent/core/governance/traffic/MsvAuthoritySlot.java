package com.hupu.msv.apm.agent.core.governance.traffic;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slotchain.AbstractLinkedProcessorSlot;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.List;

/**
 * @author: zhaoxudong
 * @date: 2019-07-26 10:16
 * @description:
 */
public class MsvAuthoritySlot extends AbstractLinkedProcessorSlot<DefaultNode> {
    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count, boolean prioritized, Object... args)
            throws Throwable {
        checkBlackWhiteAuthority(resourceWrapper, context);
        fireEntry(context, resourceWrapper, node, count, prioritized, args);
    }

    @Override
    public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
        fireExit(context, resourceWrapper, count, args);
    }

    void checkBlackWhiteAuthority(ResourceWrapper resource, Context context) throws AuthorityException {
        List<AuthorityRule> authorityRules = AuthorityRuleManager.getRules();

        if (authorityRules.isEmpty()) {
            return;
        }
        AuthorityRule rule = null;
        for (AuthorityRule authorityRule : authorityRules) {
            if (authorityRule.getResource().equals(resource.getName())) {
                rule = authorityRule;
                break;
            }
        }
        if (rule == null) {
            return;
        }
        if (!passCheck(rule, context)) {
            throw new AuthorityException(context.getOrigin(), rule);
        }
    }

    static boolean passCheck(AuthorityRule rule, Context context) {
        String origin = context.getOrigin();

        // Empty origin or empty limitApp will pass.
        if (StringUtil.isEmpty(origin) || StringUtil.isEmpty(rule.getLimitApp())) {
            return true;
        }
        String[] requesters = origin.split(",");
        boolean isExists = false;
        for (String requester : requesters) {
            // Do exact match with origin name.
            int pos = rule.getLimitApp().indexOf(requester);
            boolean contain = pos > -1;
            if (contain) {
                boolean exactlyMatch = false;
                String[] appArray = rule.getLimitApp().split(",");
                for (String app : appArray) {
                    if (requester.equals(app)) {
                        exactlyMatch = true;
                        break;
                    }
                }
                contain = exactlyMatch;
            }
            if(contain){
                isExists = true;
                break;
            }
        }

        int strategy = rule.getStrategy();
        if (strategy == RuleConstant.AUTHORITY_BLACK && isExists) {
            return false;
        }

        if (strategy == RuleConstant.AUTHORITY_WHITE && !isExists) {
            return false;
        }
        return true;
    }
}
