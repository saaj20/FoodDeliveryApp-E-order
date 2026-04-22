package edu.classproject.order;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the lifecycle states of an order.
 * Transitions are strictly enforced — no arbitrary jumps allowed.
 */
public enum OrderStatus {

    PENDING {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return EnumSet.of(CONFIRMED, CANCELLED);
        }
    },
    CONFIRMED {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return EnumSet.of(PREPARING, CANCELLED);
        }
    },
    PREPARING {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return EnumSet.of(READY_FOR_PICKUP);
        }
    },
    READY_FOR_PICKUP {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return EnumSet.of(OUT_FOR_DELIVERY);
        }
    },
    OUT_FOR_DELIVERY {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return EnumSet.of(DELIVERED);
        }
    },
    DELIVERED {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    },
    CANCELLED {
        @Override
        public Set<OrderStatus> allowedNextStates() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    };

    public abstract Set<OrderStatus> allowedNextStates();

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNextStates().contains(next);
    }
}
