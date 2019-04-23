package com.knziha.plod.PDPC; 
 
import static javafx.concurrent.Worker.State.FAILED;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListDataListener;

import com.knziha.plod.dictionary.mdict;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

@Deprecated
public class PlaneDictionaryPc extends JFrame {
 
    private final JFXPanel jfxPanel = new JFXPanel();
    private static WebEngine engine;
 
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel lblStatus = new JLabel();


    private final JButton btnGo = new JButton("Go");
    private final JTextField txtURL = new JTextField();
    private final JProgressBar progressBar = new JProgressBar();
 


    private void initComponents() {
        createScene();
 
        ActionListener al = new ActionListener() {
            @Override 
            public void actionPerformed(ActionEvent e) {
                //loadURL(txtURL.getText());
            	ListModel mode=new ListModel() {
					@Override
					public int getSize() {
						return 0;
					}

					@Override
					public Object getElementAt(int index) {
						return null;
					}

					@Override
					public void addListDataListener(ListDataListener l) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void removeListDataListener(ListDataListener l) {
						// TODO Auto-generated method stub
						
					}};
                JList list=new JList(mode);
                list.setBorder(BorderFactory.createTitledBorder("您最喜欢到哪个国家玩呢"));
            	JFrame parent = new JFrame();
                ClickAwayDialog dlg = new ClickAwayDialog(parent);
                dlg.add(new JScrollPane(list));
                dlg.setSize(500, 500);
                dlg.setVisible(true);  
                
            }
        };
        JMenuBar menubar = new JMenuBar();
        JMenu menuFile = new JMenu("文件(F)");
        menuFile.setMnemonic('F');  //设置菜单的键盘操作方式是Alt + F键
        JMenuItem itemOpen = new JMenuItem("打开(O)");
        JMenuItem itemSave = new JMenuItem("保存配置(S)");
        JMenuItem item3 = new JMenuItem("配置管理...");
        JMenuItem item4 = new JMenuItem("词典列表");
        
        
        //设置菜单项的键盘操作方式是Ctrl+O和Ctrl+S键
        KeyStroke Ctrl_cutKey = 
                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK);
        itemOpen.setAccelerator(Ctrl_cutKey);
        Ctrl_cutKey = 
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK);
        itemSave.setAccelerator(Ctrl_cutKey);

        //menubar.setMargin(new Insets(100,1,100,1));
        //menuFile.set
        menuFile.setBackground(Color.BLACK);
        menuFile.add(itemOpen);
        //menuFile.addSeparator();
        menuFile.add(itemSave);
        menubar.add(menuFile);  //将菜单添加到菜单条上
        setJMenuBar(menubar);
        itemOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
                    @Override 
                    public void run() {
        				new FileChooser().showOpenDialog(null);
                    }
                });
			}});
        btnGo.addActionListener(al);
        txtURL.addActionListener(al);
  
        progressBar.setPreferredSize(new Dimension(150, 18));
        progressBar.setStringPainted(true);
  
        JPanel topBar = new JPanel(new BorderLayout(5, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        topBar.add(txtURL, BorderLayout.CENTER);
        topBar.add(btnGo, BorderLayout.EAST);
 
        JPanel statusBar = new JPanel(new BorderLayout(5, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        statusBar.add(lblStatus, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);
 
        panel.add(topBar, BorderLayout.NORTH);
        panel.add(jfxPanel, BorderLayout.CENTER);
        //panel.add(statusBar, BorderLayout.SOUTH);
        //jfxPanel.setc
        
        getContentPane().add(panel);
        int windowWidth = 1250;
        int windowHeight = 810;
        setPreferredSize(new Dimension(windowWidth, windowHeight));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();

        Toolkit kit = Toolkit.getDefaultToolkit(); //定义工具包
        Dimension screenSize = kit.getScreenSize(); //获取屏幕的尺寸
        int screenWidth = screenSize.width; //获取屏幕的宽
        int screenHeight = screenSize.height; //获取屏幕的高
        setLocation(screenWidth/2-windowWidth/2, screenHeight/2-windowHeight/2);
        
    }
 
    private void createScene() {
 
        Platform.runLater(new Runnable() {
            @Override 
            public void run() {
                WebView view = new WebView();
                view.setFontScale(1.25);
                view.setZoom(1.25f);
                engine = view.getEngine();
                //engine.setUserAgent("");
                engine.titleProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override 
                            public void run() {
                                //PlaneDictionaryPc.this.setTitle(newValue);
                            }
                        });
                    }
                });
                engine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
                    @Override 
                    public void handle(final WebEvent<String> event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override 
                            public void run() {
                                //lblStatus.setText(event.getData());
                            }
                        });
                    }
                });
                engine.locationProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override 
                            public void run() {
                                txtURL.setText(newValue);
                            }
                        });
                    }
                });
                engine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override 
                            public void run() {
                                progressBar.setValue(newValue.intValue());
                            }
                        });
                    }
                });
                engine.getLoadWorker()
                        .exceptionProperty()
                        .addListener(new ChangeListener<Throwable>() {
                            public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
                                if (engine.getLoadWorker().getState() == FAILED) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override public void run() {
                                            JOptionPane.showMessageDialog(
                                                    panel,
                                                    (value != null) ?
                                                    engine.getLocation() + "\n" + value.getMessage() :
                                                    engine.getLocation() + "\nUnexpected error.",
                                                    "Loading error...",
                                                    JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            }
                        });
                engine.setJavaScriptEnabled(true);
                
                jfxPanel.setScene(new Scene(view));
            }
        });
    }
 
    public void loadURL(final String url) {
        Platform.runLater(new Runnable() {
            @Override 
            public void run() {
                String tmp = toURL(url);
 
                if (tmp == null) {
                    tmp = toURL("http://" + url);
                }
 
                engine.load(tmp);
            }
        });
    }
	protected void loadContent(final String content) {
		 Platform.runLater(new Runnable() {
	            @Override 
	            public void run() {
	                engine.loadContent(content);
	            }
	        });
		
	}

	protected void executeJavaScript(final String script) {
		Platform.runLater(new Runnable() {
            @Override 
            public void run() {
                engine.executeScript(script);
            }
        });
	}
    private static String toURL(String str) {
        try {
            return new URL(str).toExternalForm();
        } catch (MalformedURLException exception) {
                return null;
        }
    }

   
    
    
    //构造
    public PlaneDictionaryPc() {
        super();
		Font menuFont = new Font("Serif",Font.TRUETYPE_FONT, 20);
		
        String   names[]={ "Label", "CheckBox", "PopupMenu","MenuItem", "CheckBoxMenuItem",
    			"JRadioButtonMenuItem","ComboBox", "Button", "Tree", "ScrollPane",
    			"TabbedPane", "EditorPane", "TitledBorder", "Menu", "TextArea",
    			"OptionPane", "MenuBar", "ToolBar", "ToggleButton", "ToolTip",
    			"ProgressBar", "TableHeader", "Panel", "List", "ColorChooser",
    			"PasswordField","TextField", "Table", "Label", "Viewport",
    			"RadioButtonMenuItem","RadioButton", "DesktopPane", "InternalFrame"
    	}; 
    	for (String item : names) {
			UIManager.put(item+ ".font",menuFont ); 
    	}
    	menuFont = new Font("Serif",Font.BOLD, 20);
    	UIManager.put("Menu"+ ".font",menuFont ); 
    	
        setTitle("平典");
        //setBackground(Color.BLACK);
        setIconImage(Toolkit.getDefaultToolkit().createImage("G:\\.0PtClm\\Muse\\_All_the_spirites\\app图标\\PLOD\\launcherMax_white.png"));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    server.start();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
                //setBackground(Color.black);
                setVisible(true);
                
              
                try {
                	server.md.add(new mdict("E:\\assets\\mdicts\\LDOCE5++ V 1-31.mdx"));
                	server.md.add(new mdict("E:\\assets\\mdicts\\wordsmyth2018.mdx"));
                	loadURL("http://127.0.0.1:8080");
                	//browser.loadContent("<a onclick=\"new Audio('https://www.collinsdictionary.com/sounds/6/669/66956/66956.mp3').play();\">AUDIO TEST</a>");
                    
                	server.currentPage = server.md.get(server.adapter_idx).lookUp("wolf");
                	
                	try {
            			Thread.sleep(1000);
            		} catch (InterruptedException e) {
            			e.printStackTrace();
            		}
                    //browser.executeJavaScript("new Audio('\\sound\\asd.mp3').play();");
					//browser.loadContent(md.getRecordAt(md.lookUp("wolf")));
					//browser.loadContent("asdasd<img src='E:\\assets\\mdicts\\wordsmyth2018.png'></img>");
				} catch (IOException e) {
					e.printStackTrace();
				}
                //browser.loadURL("http://oracle.com");
           }     
        });
        initComponents();
        lblStatus.setText("哇哈哈");
    }
    
    

    static PlaneDictionaryPc browser;
    public static void main(String[] args) {
    	browser = new PlaneDictionaryPc();
    	
    	
        
    }
    
    MdictServer server = new MdictServer(8080) {};
    
    
}