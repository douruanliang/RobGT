package top.douruanliang.robgt;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * author: dourl
 * created on: 2018/9/5 下午7:03
 * description:
 */
public class GTService extends AccessibilityService {
    private static final String NOTIFICATION_HONGBAO = "恭喜发财，大吉大利";
    private static final String HONGBAO_MSG = "领取红包";
    //main
    private static final String LAUNCHER_UI = "com.being.fame.ui.activity.MainActivity";
    //chat
    private static final String OPEN_MONEY_UI = "com.being.fame.ui.activity.redpacket.RedPacketGrabActivity";
    private static final String OPEN_ID = "com.tencent.mm:id/ba_";

    private boolean mIsFromNotification = false;
    private boolean mIsMsgWindow = true;
    private PowerManager.WakeLock mWakeLock;
    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private boolean mIsOperationLock = false;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        Log.i("GT", "event" + event.getPackageName().toString());
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            List<CharSequence> text = event.getText();
            if (!text.isEmpty()) {
                for (CharSequence item : text) {
                    String t = String.valueOf(item);
                    System.out.println("通知内容：" + t);
                    if (t.contains(NOTIFICATION_HONGBAO)) {
                        mIsFromNotification = true;
                        mIsMsgWindow = true;
                        if (isKeyguardLocked()) {
                            wakeAndUnlock(true);
                        }
                        openNotification(event);
                        break;
                    }
                }
            }

        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.i("GT", "1聊天windowStateChanged界面手动点击红包消息时自动拆包");
            mIsMsgWindow = false;
            if (mIsFromNotification) {
                windowStateChanged(event);
                return;
            }
            // 聊天界面手动点击红包消息时，自动拆包
            if (OPEN_MONEY_UI.equals(event.getClassName())) {
                // 拆红包界面，点击拆红包
                Log.i("GT", "聊天界面手动点击红包消息时自动拆包");
                openHongBao();
            }

        } else if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            Log.i("GT", "聊天界面手动点击");
            openHongBao();
        }

    }

    /**
     * 界面变化
     *
     * @param event
     */
    private void windowStateChanged(AccessibilityEvent event) {

        if (LAUNCHER_UI.equals(event.getClassName())) {
            // 主界面或聊天界面，点击红包消息
            Log.i("GT", "主界面或聊天界面，点击红包消息");
            clickHongBaoMsg();
        } else if (OPEN_MONEY_UI.equals(event.getClassName())) {
            // 拆红包界面，点击拆红包
            Log.i("GT", "拆红包界面，点击拆红包");
            openHongBao();
        }
    }

    /**
     * 拆红包
     */
    private void openHongBao() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            System.out.println("GT openHongBao is null");
            return;
        }
        // 抢别人发的红包
        /*List<AccessibilityNodeInfo> openId = nodeInfo
                .findAccessibilityNodeInfosByViewId(@id/itemContent);*/
        List<AccessibilityNodeInfo> openId = nodeInfo
                .findAccessibilityNodeInfosByText("開");

        if (openId != null && openId.size() > 0) {
            openId.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.i("GT", "coming---拆红包-----");
        }
        if (mIsOperationLock) {
            wakeAndUnlock(false);
        }
        // 抢没抢到红包都要把变量赋为false，以便后续继续拆从通知栏进来的红包消息
        mIsFromNotification = false;

    }

    private void openNotification(AccessibilityEvent event) {
        if (event.getParcelableData() == null
                || !(event.getParcelableData() instanceof Notification)) {
            return;
        }

        // 获取通知栏的意图并打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        try {
            pendingIntent.send();
            // 只有接收到通知并且窗口没有变化时才需要点击红包消息
            // 因为此时是在微信主界面
            if (mIsMsgWindow) {
                clickHongBaoMsg();
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private boolean isKeyguardLocked() {
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return mKeyguardManager.isKeyguardLocked();
    }

    private void wakeAndUnlock(boolean unlock) {
        if (unlock) {
            PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = manager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "GT");
            mWakeLock.acquire();
            if (mKeyguardManager != null) {
                mKeyguardLock = mKeyguardManager.newKeyguardLock("unLock");
                mKeyguardLock.disableKeyguard();
                mIsOperationLock = true;
            }
        } else {
            if (mKeyguardLock != null) {
                mKeyguardLock.reenableKeyguard();
                mIsOperationLock = false;
            }
            if (mWakeLock != null) {
                mWakeLock.release();
            }
        }
    }

    private void clickHongBaoMsg() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            System.out.println("GT rootwindow is null");
            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo
                .findAccessibilityNodeInfosByText(HONGBAO_MSG);
        if (list.size() >= 1) {
            // 只拆最后一个红包
            AccessibilityNodeInfo item = list.get(list.size() - 1);
            AccessibilityNodeInfo parent = item.getParent();
            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.i("GT", "coming---click-----");
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        Log.i("GT", "coming---onServiceConnected-----");
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        setServiceInfo(info);
        info.packageNames = new String[]{"com.game.me", "com.game.me.dev"};
        setServiceInfo(info);


        super.onServiceConnected();
    }
}
