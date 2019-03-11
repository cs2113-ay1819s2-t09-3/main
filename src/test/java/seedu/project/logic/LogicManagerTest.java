package seedu.project.logic;

import static org.junit.Assert.assertEquals;
import static seedu.project.commons.core.Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
import static seedu.project.commons.core.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.project.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.project.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.project.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.project.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.project.testutil.TypicalTasks.AMY;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import seedu.project.logic.commands.AddCommand;
import seedu.project.logic.commands.CommandResult;
import seedu.project.logic.commands.HistoryCommand;
import seedu.project.logic.commands.ListCommand;
import seedu.project.logic.commands.exceptions.CommandException;
import seedu.project.logic.parser.exceptions.ParseException;
import seedu.project.model.Model;
import seedu.project.model.ModelManager;
import seedu.project.model.ReadOnlyProjectList;
import seedu.project.model.project.ReadOnlyProject;
import seedu.project.model.UserPrefs;
import seedu.project.model.task.Task;
import seedu.project.storage.JsonProjectListStorage;
import seedu.project.storage.JsonProjectStorage;
import seedu.project.storage.JsonUserPrefsStorage;
import seedu.project.storage.StorageManager;
import seedu.project.testutil.TaskBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy exception");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Model model = new ModelManager();
    private Logic logic;

    @Before
    public void setUp() throws Exception {
        JsonProjectListStorage projectListStorage = new JsonProjectListStorage(temporaryFolder.newFile().toPath());
        JsonProjectStorage projectStorage = new JsonProjectStorage(temporaryFolder.newFile().toPath());
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.newFile().toPath());
        StorageManager storage = new StorageManager(projectListStorage, projectStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
        assertHistoryCorrect(invalidCommand);
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        assertHistoryCorrect(deleteCommand);
    }

    @Test
    public void execute_validCommand_success() {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_SUCCESS, model);
        assertHistoryCorrect(listCommand);
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() throws Exception {
        // Setup LogicManager with JsonProjectIoExceptionThrowingStub
        JsonProjectListStorage projectListStorage = new JsonProjectListIoExceptionThrowingStub(temporaryFolder.newFile().toPath());
        JsonProjectStorage projectStorage = new JsonProjectIoExceptionThrowingStub(temporaryFolder.newFile().toPath());
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.newFile().toPath());
        StorageManager storage = new StorageManager(projectListStorage, projectStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);

        // Execute add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY + EMAIL_DESC_AMY
                + ADDRESS_DESC_AMY;
        Task expectedTask = new TaskBuilder(AMY).withTags().build();
        ModelManager expectedModel = new ModelManager();
        expectedModel.addTask(expectedTask);
        expectedModel.commitProject();
        String expectedMessage = LogicManager.FILE_OPS_ERROR_MESSAGE + DUMMY_IO_EXCEPTION;
        assertCommandBehavior(CommandException.class, addCommand, expectedMessage, expectedModel);
        assertHistoryCorrect(addCommand);
    }

    @Test
    public void getFilteredTaskList_modifyList_throwsUnsupportedOperationException() {
        thrown.expect(UnsupportedOperationException.class);
        logic.getFilteredTaskList().remove(0);
    }

    /**
     * Executes the command, confirms that no exceptions are thrown and that the
     * result message is correct. Also confirms that {@code expectedModel} is as
     * specified.
     *
     * @see #assertCommandBehavior(Class, String, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage, Model expectedModel) {
        assertCommandBehavior(null, inputCommand, expectedMessage, expectedModel);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the
     * result message is correct.
     *
     * @see #assertCommandBehavior(Class, String, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the
     * result message is correct.
     *
     * @see #assertCommandBehavior(Class, String, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the
     * result message is correct.
     *
     * @see #assertCommandBehavior(Class, String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<?> expectedException, String expectedMessage) {
        Model expectedModel = new ModelManager(model.getProject(), new UserPrefs());
        assertCommandBehavior(expectedException, inputCommand, expectedMessage, expectedModel);
    }

    /**
     * Executes the command, confirms that the result message is correct and that
     * the expected exception is thrown, and also confirms that the following two
     * parts of the LogicManager object's state are as expected:<br>
     * - the internal model manager data are same as those in the
     * {@code expectedModel} <br>
     * - {@code expectedModel}'s project was saved to the storage file.
     */
    private void assertCommandBehavior(Class<?> expectedException, String inputCommand, String expectedMessage,
            Model expectedModel) {

        try {
            CommandResult result = logic.execute(inputCommand);
            assertEquals(expectedException, null);
            assertEquals(expectedMessage, result.getFeedbackToUser());
        } catch (CommandException | ParseException e) {
            assertEquals(expectedException, e.getClass());
            assertEquals(expectedMessage, e.getMessage());
        }

        assertEquals(expectedModel, model);
    }

    /**
     * Asserts that the result display shows all the {@code expectedCommands} upon
     * the execution of {@code HistoryCommand}.
     */
    private void assertHistoryCorrect(String... expectedCommands) {
        try {
            CommandResult result = logic.execute(HistoryCommand.COMMAND_WORD);
            String expectedMessage = String.format(HistoryCommand.MESSAGE_SUCCESS, String.join("\n", expectedCommands));
            assertEquals(expectedMessage, result.getFeedbackToUser());
        } catch (ParseException | CommandException e) {
            throw new AssertionError("Parsing and execution of HistoryCommand.COMMAND_WORD should succeed.", e);
        }
    }

    /**
     * A stub class to throw an {@code IOException} when the save method is called.
     */
    private static class JsonProjectIoExceptionThrowingStub extends JsonProjectStorage {
        private JsonProjectIoExceptionThrowingStub(Path filePath) {
            super(filePath);
        }

        @Override
        public void saveProject(ReadOnlyProject project, Path filePath) throws IOException {
            throw DUMMY_IO_EXCEPTION;
        }
    }

    /**
     * A stub class to throw an {@code IOException} when the save method is called.
     */
    private static class JsonProjectListIoExceptionThrowingStub extends JsonProjectListStorage {
        private JsonProjectListIoExceptionThrowingStub(Path filePath) {
            super(filePath);
        }

        @Override
        public void saveProjectList(ReadOnlyProjectList projectList, Path filePath) throws IOException {
            throw DUMMY_IO_EXCEPTION;
        }
    }
}
