package seedu.project.model.project;

import javafx.beans.Observable;
import javafx.collections.ObservableList;
import seedu.project.model.Name;
import seedu.project.model.task.Task;

/**
 * Unmodifiable view of an project
 */
public interface ReadOnlyProject extends Observable {

    /**
     * Returns an unmodifiable view of the tasks list.
     * This list will not contain any duplicate tasks.
     */
    ObservableList<Task> getTaskList();

    Name getName();

    /**
     * Returns index of a {@code taskID} in the list.
     * {@code taskID} must exist in the project
     */
    int getIndex(int taskId);

}
