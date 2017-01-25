package co.kartoo.app.rest.model.newest;

/**
 * Created by Luthfi Apriyanto on 12/19/2016.
 */

public class PopupReviewObject {
    String id;
    String feedbackContent;
    String feedbackCheckList;

    public PopupReviewObject() {

    }

    public PopupReviewObject(String id, String feedbackContent, String feedbackCheckList) {
        this.id = id;
        this.feedbackContent = feedbackContent;
        this.feedbackCheckList = feedbackCheckList;

    }
}