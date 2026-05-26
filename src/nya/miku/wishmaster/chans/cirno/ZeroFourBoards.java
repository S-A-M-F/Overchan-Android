package nya.miku.wishmaster.chans.cirno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nya.miku.wishmaster.api.models.BoardModel;
import nya.miku.wishmaster.api.models.SimpleBoardModel;

public class ZeroFourBoards {
    private static final String[] ATTACHMENT_FILTERS = new String[] { "jpg", "jpeg", "png", "gif", "webm", "mp4", "ogv" };

    private static final List<BoardModel> LIST = new ArrayList<BoardModel>();
    private static final Map<String, BoardModel> MAP = new HashMap<String, BoardModel>();
    private static final SimpleBoardModel[] SIMPLE_ARRAY;

    static {
        addBoard("d", "/d/епо", "Работа сайта", "Девочка", false);
        addBoard("b", "/b/улочная", "Общее", "Девочка", true);

        SIMPLE_ARRAY = new SimpleBoardModel[LIST.size()];
        for (int i=0; i<LIST.size(); ++i) SIMPLE_ARRAY[i] = new SimpleBoardModel(LIST.get(i));
    }

    static BoardModel getBoard(String boardName) {
        BoardModel board = MAP.get(boardName);
        if (board == null) return createDefaultBoardModel(boardName, boardName, null, "Девочка", false);
        return board;
    }

    static SimpleBoardModel[] getBoardsList() {
        return SIMPLE_ARRAY;
    }

    private static void addBoard(String name, String description, String category, String defaultPosterName, boolean nsfw) {
        BoardModel model = createDefaultBoardModel(name, description, category, defaultPosterName, nsfw);
        LIST.add(model);
        MAP.put(name, model);
    }

    private static BoardModel createDefaultBoardModel(String name, String description, String category, String defaultPosterName, boolean nsfw) {
        BoardModel model = new BoardModel();
        model.chan = ZeroFourModule.ZEROFOUR_NAME;
        model.boardName = name;
        model.boardDescription = description;
        model.boardCategory = category;
        model.nsfw = nsfw;
        model.uniqueAttachmentNames = true;
        model.timeZoneId = "GMT+3";
        model.defaultUserName = defaultPosterName;
        model.bumpLimit = 500;

        model.readonlyBoard = name.equals("d");
        model.requiredFileForNewThread = !name.equals("d");
        model.allowDeletePosts = !name.equals("d");
        model.allowDeleteFiles = model.allowDeletePosts;
        model.allowReport = BoardModel.REPORT_SIMPLE;
        model.allowNames = !name.equals("b");
        model.allowSubjects = true;
        model.allowSage = true;
        model.allowEmails = false;
        model.ignoreEmailIfSage = false;
        model.allowCustomMark = false;
        model.allowRandomHash = true;
        model.allowIcons = false;
        model.attachmentsMaxCount = name.equals("d") ? 0 : 1;
        model.attachmentsFormatFilters = ATTACHMENT_FILTERS;
        model.markType = BoardModel.MARK_WAKABAMARK;

        model.firstPage = 0;
        model.lastPage = BoardModel.LAST_PAGE_UNDEFINED;
        model.catalogAllowed = !name.equals("d");
        return model;
    }

}
