package com.techtune.nexus.worker.handlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobHandlerRegistryTest {

    @Test
    void shouldRegisterHandler() {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        JobHandler handler = new TestHandler("test-job");
        
        registry.register(handler);
        
        assertTrue(registry.hasHandler("test-job"));
        assertEquals(handler, registry.getHandler("test-job"));
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        JobHandler handler1 = new TestHandler("test-job");
        JobHandler handler2 = new TestHandler("test-job");
        
        registry.register(handler1);
        
        assertThrows(IllegalStateException.class, () -> registry.register(handler2));
    }

    @Test
    void shouldRejectBlankJobType() {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        JobHandler handler = new TestHandler("");
        
        assertThrows(IllegalArgumentException.class, () -> registry.register(handler));
    }

    @Test
    void shouldRejectNullJobType() {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        JobHandler handler = new TestHandler(null);
        
        assertThrows(IllegalArgumentException.class, () -> registry.register(handler));
    }

    @Test
    void shouldThrowForUnknownJobType() {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        
        assertThrows(IllegalArgumentException.class, () -> registry.getHandler("unknown"));
    }

    @Test
    void shouldFreezeAfterFrozen() {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        registry.freeze();
        
        JobHandler handler = new TestHandler("new-job");
        assertThrows(IllegalStateException.class, () -> registry.register(handler));
    }

    @Test
    void shouldReturnRegisteredJobTypes() {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        registry.register(new TestHandler("job-1"));
        registry.register(new TestHandler("job-2"));
        
        var types = registry.getRegisteredJobTypes();
        assertEquals(2, types.size());
        assertTrue(types.contains("job-1"));
        assertTrue(types.contains("job-2"));
    }

    private static class TestHandler implements JobHandler {
        private final String jobType;

        TestHandler(String jobType) {
            this.jobType = jobType;
        }

        @Override
        public String getJobType() {
            return jobType;
        }

        @Override
        public void handle(com.techtune.nexus.job.domain.Job job) throws Exception {
            // No-op
        }
    }
}
