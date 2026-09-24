import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class MainClass {
	public static void main(String[] args) {
		MainCanvas meuCanvas = new MainCanvas();

		JFrame f = new JFrame("Leitor OBJ");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().add(meuCanvas);
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);

		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		meuCanvas.requestFocusInWindow();
		meuCanvas.start();
	}
}
