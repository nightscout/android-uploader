package com.nightscout.android.tutorial;

import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.nightscout.android.MainActivity;
import com.nightscout.android.R;

public class Tutorial {

    private MainActivity mActivity;
    private int TUTORIAL_VERSION = 1;

    public Tutorial(MainActivity activity) {
        mActivity = activity;
    }

    public void startTutorial() {
        showOverlayTutorialOne();
    }

    private void showOverlayTutorialOne() {
        new ShowcaseView.Builder(mActivity)
            .setContentTitle(R.string.tutorial_one_title)
            .setContentText(R.string.tutorial_one_summary)
            .singleShot(TUTORIAL_VERSION)
            .setShowcaseEventListener(new OnShowcaseEventListener() {

                @Override
                public void onShowcaseViewShow(final ShowcaseView scv) {
                    scv.setButtonText("Next");
                }

                @Override
                public void onShowcaseViewHide(final ShowcaseView scv) {
                    scv.setVisibility(View.GONE);
                    showOverlayTutorialTwo();
                }

                @Override
                public void onShowcaseViewDidHide(final ShowcaseView scv) {
                }

            })
            .build();
    }

    private void showOverlayTutorialTwo() {
        new ShowcaseView.Builder(mActivity)
            .setContentTitle(R.string.tutorial_two_title)
            .setContentText(R.string.tutorial_two_summary)
            .setTarget(new ViewTarget(R.id.sgValue, mActivity))
            .setShowcaseEventListener(new OnShowcaseEventListener() {

                @Override
                public void onShowcaseViewShow(final ShowcaseView scv) {
                    scv.setButtonText("Next");
                }

                @Override
                public void onShowcaseViewHide(final ShowcaseView scv) {
                    scv.setVisibility(View.GONE);
                    showOverlayTutorialThree();
                }

                @Override
                public void onShowcaseViewDidHide(final ShowcaseView scv) {
                }

            })
            .build();
    }

    private void showOverlayTutorialThree() {
        new ShowcaseView.Builder(mActivity)
            .setContentTitle(R.string.tutorial_three_title)
            .setContentText(R.string.tutorial_three_summary)
            .setTarget(new ViewTarget(R.id.timeAgo, mActivity))
            .setShowcaseEventListener(new OnShowcaseEventListener() {

                @Override
                public void onShowcaseViewShow(final ShowcaseView scv) {
                    scv.setButtonText("Next");
                }

                @Override
                public void onShowcaseViewHide(final ShowcaseView scv) {
                    scv.setVisibility(View.GONE);
                    showOverlayTutorialFour();
                }

                @Override
                public void onShowcaseViewDidHide(final ShowcaseView scv) { }

            })
            .build();
    }

    private void showOverlayTutorialFour() {
        new ShowcaseView.Builder(mActivity)
            .setContentTitle(R.string.tutorial_four_title)
            .setContentText(R.string.tutorial_four_summary)
            .setTarget(new ViewTarget(R.id.imageViewUSB, mActivity))
            .setShowcaseEventListener(new OnShowcaseEventListener() {

                @Override
                public void onShowcaseViewShow(final ShowcaseView scv) {
                    scv.setButtonText("Next");
                }

                @Override
                public void onShowcaseViewHide(final ShowcaseView scv) {
                    scv.setVisibility(View.GONE);
                    showOverlayTutorialFive();
                }

                @Override
                public void onShowcaseViewDidHide(final ShowcaseView scv) { }

            })
            .build();
    }

    private void showOverlayTutorialFive() {
        new ShowcaseView.Builder(mActivity)
            .setContentTitle(R.string.tutorial_five_title)
            .setContentText(R.string.tutorial_five_summary)
            .setTarget(new ViewTarget(R.id.imageViewUploadStatus, mActivity))
            .setShowcaseEventListener(new OnShowcaseEventListener() {

                @Override
                public void onShowcaseViewShow(final ShowcaseView scv) {
                    scv.setButtonText("Next");
                }

                @Override
                public void onShowcaseViewHide(final ShowcaseView scv) {
                    scv.setVisibility(View.GONE);
                    showOverlayTutorialSix();
                }

                @Override
                public void onShowcaseViewDidHide(final ShowcaseView scv) { }

            })
            .build();
    }

    private void showOverlayTutorialSix() {
        new ShowcaseView.Builder(mActivity)
            .setContentTitle(R.string.tutorial_six_title)
            .setContentText(R.string.tutorial_six_summary)
            .setTarget(new ViewTarget(R.id.imageViewRcvrBattery, mActivity))
            .setShowcaseEventListener(new OnShowcaseEventListener() {

                @Override
                public void onShowcaseViewShow(final ShowcaseView scv) {
                    scv.setButtonText("Next");
                }

                @Override
                public void onShowcaseViewHide(final ShowcaseView scv) {
                    scv.setVisibility(View.GONE);
                    showOverlayTutorialSeven();
                }

                @Override
                public void onShowcaseViewDidHide(final ShowcaseView scv) { }

            })
            .build();
    }

    private void showOverlayTutorialSeven() {
        new ShowcaseView.Builder(mActivity)
            .setContentTitle(R.string.tutorial_seven_title)
            .setContentText(R.string.tutorial_seven_summary)
            .setTarget(new ViewTarget(R.id.imageViewTimeIndicator, mActivity))
            .setShowcaseEventListener(new OnShowcaseEventListener() {

                @Override
                public void onShowcaseViewShow(final ShowcaseView scv) {
                    scv.setButtonText("Next");
                    int margin = (int) mActivity.getApplicationContext().getResources().getDimension(R.dimen.button_margin);
                    RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    lps.setMargins(margin, margin, margin, margin);
                    scv.setButtonPosition(lps);
                }

                @Override
                public void onShowcaseViewHide(final ShowcaseView scv) {
                    scv.setVisibility(View.GONE);
                    showOverlayTutorialEight();
                }

                @Override
                public void onShowcaseViewDidHide(final ShowcaseView scv) { }

            })
            .build();
    }

    private void showOverlayTutorialEight() {
        new ShowcaseView.Builder(mActivity)
            .setContentTitle(R.string.tutorial_eight_title)
            .setContentText(R.string.tutorial_eight_summary)
            .setShowcaseEventListener(new OnShowcaseEventListener() {

                @Override
                public void onShowcaseViewShow(final ShowcaseView scv) {
                    scv.setButtonText("Done");
                }

                @Override
                public void onShowcaseViewHide(final ShowcaseView scv) {
                    scv.setVisibility(View.GONE);
                }

                @Override
                public void onShowcaseViewDidHide(final ShowcaseView scv) { }

            })
            .build();
    }
}
