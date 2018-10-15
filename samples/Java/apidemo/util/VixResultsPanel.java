/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import apidemo.MarketDataPanel.TopResultsPanel;
import com.ib.controller.NewContract; 

/**
 *
 * @author heigold1
 */
public class VixResultsPanel {
    public NewContract m_vixContract;
    public TopResultsPanel m_resultsPanel;
    public int m_reqId; 
    
    public VixResultsPanel(int reqId, NewContract contract, TopResultsPanel resultsPanel)
    {
        m_vixContract = contract;
        m_resultsPanel = resultsPanel;
        m_reqId = reqId; 
    }    
}
