package com.lexandera.mosembro.jsinterfaces;

import org.json.JSONArray;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.ClipboardManager;

import com.lexandera.mosembro.Mosembro;
import com.lexandera.mosembro.SmartAction;
import com.lexandera.mosembro.dialogs.SmartActionsDialog;

/** 
 * This JS interface handles window.ActionInterface.execute(id) calls which are
 * triggered by onclick events, attached to "smart links" (when smart links are enabled)
 */
public class ActionInterface
{
    Mosembro browser;
    int actionGroupId = 0;
    
    public ActionInterface(Mosembro browser)
    {
        this.browser = browser;
    }
    
    public String getScriptsFor(String scriptSecretKey, String category)
    {
        if (!browser.isValidScriptKey(scriptSecretKey)) {
            return "";
        }
        // TODO: cache scripts!
        
        JSONArray jsa = new JSONArray();
        
        String[] actions = browser.getActionStore().getStriptsForMicroformatActions(category);
        for (int i=0; i<actions.length; i++) {
            jsa.put(actions[i]);
        }
        
        return jsa.toString();
    }
    
    public int startNewActionGroup(String scriptSecretKey)
    {
        if (!browser.isValidScriptKey(scriptSecretKey)) {
            return 0;
        }
        
        return ++actionGroupId;
    }
    
    public boolean addAction(String scriptSecretKey, 
            final String actionId, final String action, final String value, 
            final String descShort, final String descLong)
    {
        if (!browser.isValidScriptKey(scriptSecretKey)) {
            return false;
        }
        
        final SmartAction sa = new SmartAction()
        {
            @Override
            public void execute()
            {
                String intentAction = null;
                
                if ("TEXT_COPY".equals(action)) {
                    ClipboardManager clipboard = (ClipboardManager)browser.getSystemService(Context.CLIPBOARD_SERVICE); 
                    clipboard.setText(value);
                }
                else if ("RUN_JAVASCRIPT".equals(action)) {
                    browser.getWebView().loadUrl("javascript:(function(){ " + value + " })()");
                }
                else {
                    try {
                        intentAction = (String)Intent.class.getField(action).get(null);
                    }
                    catch (Exception e) {}
                    
                    Intent i = new Intent(intentAction, Uri.parse(value));
                    browser.startActivity(i);
                }
            }
            
            @Override
            public String getLongDescription()
            {
                return descLong;
            }
            
            @Override
            public String getShortDescription()
            {
                return descShort;
            }
            
            @Override
            public Bitmap getIconBitmap()
            {
                return browser.getActionStore().getIconForAction(actionId);
            }
        };
        
        browser.addSmartAction(sa, actionGroupId);
        browser.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                browser.updateTitleIcons();
            }});
        
        if (browser.getEnableContentRewriting()) {
            return true;
        };
        
        return false;
    }
    
    public void showActionGroupDialog(int groupId)
    {
        new SmartActionsDialog(browser, groupId).show();
    }
    
    public String actionGroupLink(String scriptSecretKey, int groupId, String text)
    {
        if (!browser.isValidScriptKey(scriptSecretKey)) {
            return "";
        }
        
        return "<div style=\"display: block; clear: both; margin: 5px 5px 5px 2px; font-size: 85%;\">"+
            "<a href=\"/null\" " +
            "onclick=\"window.ActionInterface.showActionGroupDialog("+Integer.toString(groupId)+"); " +
            "return false;\">" + text + 
            "</a>" +
            "</div>";
    }
}
