package com.github.alinz.reactnativewebviewbridge;

import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.webview.ReactWebViewManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class WebViewBridgeManager extends ReactWebViewManager {
    private static final String REACT_CLASS = "RCTWebViewBridge";

    public static final int COMMAND_SEND_TO_BRIDGE = 101;
    private List<String> _menuItems;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public
    @Nullable
    Map<String, Integer> getCommandsMap() {
        Map<String, Integer> commandsMap = super.getCommandsMap();

        commandsMap.put("sendToBridge", COMMAND_SEND_TO_BRIDGE);

        return commandsMap;
    }

    @Override
    protected WebView createViewInstance(ThemedReactContext reactContext) {
        final WebView root = getViewInstance(reactContext);
        root.addJavascriptInterface(new JavascriptBridge(root), "WebViewBridge");
        return root;
    }

    @Override
    public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
        super.receiveCommand(root, commandId, args);

        switch (commandId) {
            case COMMAND_SEND_TO_BRIDGE:
                sendToBridge(root, args.getString(0));
                break;
            default:
                //do nothing!!!!
        }
    }

    private void sendToBridge(WebView root, String message) {
        String script = "WebViewBridge.onMessage('" + message + "');";
        WebViewBridgeManager.evaluateJavascript(root, script);
    }

    static private void evaluateJavascript(WebView root, String javascript) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            root.evaluateJavascript(javascript, null);
        } else {
            root.loadUrl("javascript:" + javascript);
        }
    }

    @ReactProp(name = "allowFileAccessFromFileURLs")
    public void setAllowFileAccessFromFileURLs(WebView root, boolean allows) {
        root.getSettings().setAllowFileAccessFromFileURLs(allows);
    }

    @ReactProp(name = "allowUniversalAccessFromFileURLs")
    public void setAllowUniversalAccessFromFileURLs(WebView root, boolean allows) {
        root.getSettings().setAllowUniversalAccessFromFileURLs(allows);
    }

    @ReactProp(name = "menuItems")
    public void setMenuItems(WebView root, ReadableArray items) {
        List<String> result = new ArrayList<String>(items.size());
        for (int i = 0; i < items.size(); i++) {
            result.add(items.getString(i));
        }

        this._menuItems = result;
    }

    private WebView getViewInstance(ThemedReactContext reactContext) {
        MyReactWebView webView = new MyReactWebView(reactContext);
        reactContext.addLifecycleEventListener(webView);
        mWebViewConfig.configWebView(webView);

        if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        reactContext.getCurrentActivity().registerForContextMenu(webView);

        return webView;
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        Map export = super.getExportedCustomDirectEventTypeConstants();
        if (export == null) {
            export = MapBuilder.newHashMap();
        }
        export.put("onMenuItemSelected", MapBuilder.of("registrationName", "onMenuItemSelected"));
        return export;
    }

    private class MyReactWebView extends ReactWebView {

        public MyReactWebView(ThemedReactContext reactContext) {
            super(reactContext);
        }

        @Override
        public ActionMode startActionMode(final ActionMode.Callback callback, int type) {

            if (_menuItems == null || _menuItems.size() == 0 || type != ActionMode.TYPE_FLOATING)
                return super.startActionMode(callback, type);

            return super.startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    boolean prev = callback.onCreateActionMode(actionMode, menu);
                    for (String item : _menuItems) {
                        menu.add(item);
                    }
//                    menu.add(0,0,0, "Add Lero");
//                    for (int i = 0; i < _menuItems.length; i++) {
//                        menu.add(_menuItems[i]);
//                    }
                    return prev;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    return callback.onPrepareActionMode(actionMode, menu);
                }

                @Override
                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

                    if (!_menuItems.contains(menuItem.getTitle())){
                        return callback.onActionItemClicked(actionMode, menuItem);
                    }

                    WritableMap event = Arguments.createMap();
                    event.putString("eventType",
                        _menuItems.get(_menuItems.indexOf(menuItem.getTitle())));

                    ThemedReactContext reactContext = (ThemedReactContext)getContext();
                    reactContext.getJSModule(RCTEventEmitter.class)
                            .receiveEvent(getId(), "onMenuItemSelected", event);

                    actionMode.finish();

                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    callback.onDestroyActionMode(actionMode);
                }
            }, type);
        }
    }
}