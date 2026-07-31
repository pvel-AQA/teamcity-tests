package ui.elements;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ProjectElementOnAdminPage {
    private String name;
    private int indentLevel;
    private boolean expanded;
    private List<ProjectElementOnAdminPage> children;

    public ProjectElementOnAdminPage(String name, int indentLevel) {
        this.name = name;
        this.indentLevel = indentLevel;
        this.expanded = false;
        this.children = new ArrayList<>();
    }

    public ProjectElementOnAdminPage(String name, int indentLevel, boolean expanded) {
        this.name = name;
        this.indentLevel = indentLevel;
        this.expanded = expanded;
        this.children = new ArrayList<>();
    }

    public void addChild(ProjectElementOnAdminPage child) {
        this.children.add(child);
    }
}
