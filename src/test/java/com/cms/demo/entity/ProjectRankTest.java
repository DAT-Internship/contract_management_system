package com.cms.demo.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectRankTest {

    @Test
    void testValidProjectRanks() {
        Project project = new Project();

        project.setProjectRank("A");
        assertEquals("A", project.getProjectRank());

        project.setProjectRank("b"); // auto uppercase
        assertEquals("B", project.getProjectRank());

        project.setProjectRank("C");
        assertEquals("C", project.getProjectRank());

        project.setProjectRank(null);
        assertNull(project.getProjectRank());

        project.setProjectRank("  ");
        assertNull(project.getProjectRank());
    }

    @Test
    void testInvalidProjectRanksThrowException() {
        Project project = new Project();

        assertThrows(IllegalArgumentException.class, () -> project.setProjectRank("D"));
        assertThrows(IllegalArgumentException.class, () -> project.setProjectRank("X"));
        assertThrows(IllegalArgumentException.class, () -> project.setProjectRank("1"));
        assertThrows(IllegalArgumentException.class, () -> project.setProjectRank("AB"));
    }
}
