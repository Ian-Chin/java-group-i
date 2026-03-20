package view;

import model.AccountService;
import model.User;

import javax.swing.*;
import java.awt.*;

public class AppFrame extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private String loggedInUser = "";
    private User loggedInUserObj;
    private final AccountService accountService;

    public AppFrame() {
        accountService = new AccountService();

        setTitle("APU Automotive Service Centre");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(500, 660));
        getContentPane().setBackground(UIConstants.BG_PAGE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(UIConstants.BG_PAGE);

        // Add all pages as cards
        mainPanel.add(new OnboardingPage(this), PageName.ONBOARDING.name());
        mainPanel.add(new CreateAccountPage(this), PageName.CREATE_ACCOUNT.name());
        mainPanel.add(new LoginPage(this), PageName.LOGIN.name());
        mainPanel.add(new AdminDashboard(this), PageName.DASHBOARD.name());

        add(mainPanel);
        showPage(PageName.ONBOARDING);
        setVisible(true);
    }

    public void showPage(PageName page) {
        cardLayout.show(mainPanel, page.name());
        if (page == PageName.DASHBOARD) {
            for (Component c : mainPanel.getComponents()) {
                if (c instanceof AdminDashboard) {
                    ((AdminDashboard) c).refreshUser();
                    break;
                }
            }
        }
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public void setLoggedInUser(String name) {
        this.loggedInUser = name;
    }

    public String getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUserObj(User user) {
        this.loggedInUserObj = user;
    }

    public User getLoggedInUserObj() {
        return loggedInUserObj;
    }
}
