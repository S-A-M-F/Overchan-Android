package nya.miku.wishmaster.chans.gensokyo;

import nya.miku.wishmaster.api.models.BoardModel;
import nya.miku.wishmaster.api.models.SimpleBoardModel;

public class GensokyoBoards {
    private static final String[] ATTACHMENT_FILTERS = new String[] { "jpg", "jpeg", "png", "gif", "webp" };

    private static BoardModel createBoard(String name, String description, String category, String defaultName, boolean nsfw, boolean readonly) {
        BoardModel model = new BoardModel();
        model.chan = GensokyoModule.CHAN_NAME;
        model.boardName = name;
        model.boardDescription = description;
        model.boardCategory = category;
        model.nsfw = nsfw;
        model.uniqueAttachmentNames = true;
        model.timeZoneId = "GMT+3";
        model.defaultUserName = defaultName;
        model.bumpLimit = 500;
        model.readonlyBoard = readonly;
        model.requiredFileForNewThread = true;
        model.allowDeletePosts = !readonly;
        model.allowDeleteFiles = !readonly;
        model.allowReport = BoardModel.REPORT_NOT_ALLOWED;
        model.allowNames = true;
        model.allowSubjects = true;
        model.allowSage = false;
        model.allowEmails = false;
        model.ignoreEmailIfSage = false;
        model.allowCustomMark = false;
        model.allowRandomHash = true;
        model.allowIcons = false;
        model.attachmentsMaxCount = 1;
        model.attachmentsFormatFilters = ATTACHMENT_FILTERS;
        model.markType = BoardModel.MARK_WAKABAMARK;
        model.firstPage = 0;
        model.lastPage = BoardModel.LAST_PAGE_UNDEFINED;
        model.catalogAllowed = true;
        return model;
    }

    public static BoardModel getBoard(String shortName) {
        if ("b".equals(shortName)) return createBoard("b", "Живая борда", "Gensokyo RPGs", "Сырно", false, false);
        if ("arch_b".equals(shortName)) return createBoard("arch_b", "Архив", "Gensokyo RPGs", "Сырно", false, true);
        return null;
    }

    public static SimpleBoardModel[] getBoardsList() {
        BoardModel b = getBoard("b");
        BoardModel ab = getBoard("arch_b");
        return new SimpleBoardModel[] {
            new SimpleBoardModel(b),
            new SimpleBoardModel(ab)
        };
    }
}
