/*
 * Copyright 2018 Heiko Scherrer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openwms.tms.state;

import org.openwms.tms.Message;
import org.openwms.tms.StateChangeException;
import org.openwms.tms.TransportOrder;
import org.openwms.tms.TransportServiceEvent;
import org.openwms.tms.UpdateFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A ChangeState is an {@link UpdateFunction} to change the state of an {@link TransportOrder}.
 *
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 * @see org.openwms.tms.UpdateFunction
 */
@Transactional(propagation = Propagation.MANDATORY)
@Component
class ChangeState implements UpdateFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeState.class);
    private final ApplicationContext ctx;

    ChangeState(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(TransportOrder saved, TransportOrder toUpdate) {
        if (saved.getState() != toUpdate.getState() && toUpdate.getState() != null) {
            try {
                LOGGER.debug("Trying to turn TransportOrder [{}] into state [{}]", saved.getPk(), toUpdate.getState());
                saved.changeState(toUpdate.getState());
                ctx.publishEvent(new TransportServiceEvent(saved.getPk(), TransportServiceEvent.TYPE.of(toUpdate.getState())));
            } catch (StateChangeException sce) {
                LOGGER.error("Could not turn TransportOrder: [{}] into [{}], because of [{}]", saved.getPk(), toUpdate.getState(), sce.getMessage());
                Message problem = new Message.Builder().withMessage(sce.getMessage()).build();
                saved.setProblem(problem);
            }
        }
    }
}
