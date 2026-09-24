import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import core3d.Camera;
import core3d.Mat4x4;
import core3d.Ponto3D;
import obj.ObjLoader;
import obj.ObjModel;
import obj.SceneObject;

public class MainCanvas extends JPanel implements Runnable {
	private static final long serialVersionUID = 1L;

	int W = 960;
	int H = 640;

	Thread runner;
	boolean ativo = true;

	int framecount = 0;
	int fps = 0;

	Font fonteHud = new Font("Monospaced", Font.PLAIN, 14);

	Camera camera = new Camera();

	List<SceneObject> objetos = new ArrayList<>();

	boolean moveFrente, moveTras, moveEsquerda, moveDireita, moveCima, moveBaixo;
	boolean olharEsquerda, olharDireita, olharCima, olharBaixo;

	boolean arrastando = false;
	int ultimoMouseX, ultimoMouseY;

	static final float NEAR = 0.15f;
	static final float FOV_GRAUS = 70f;
	static final float VEL_MOVIMENTO = 8f;
	static final float VEL_OLHAR_TECLADO = 90f;
	static final float VEL_OLHAR_MOUSE = 0.2f;

	public MainCanvas() {
		setPreferredSize(new java.awt.Dimension(W, H));
		setSize(W, H);
		setFocusable(true);

		carregarCena();

		addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
				trataTecla(e.getKeyCode(), true);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				trataTecla(e.getKeyCode(), false);
			}
		});

		addMouseListener(new MouseListener() {
			@Override public void mouseReleased(MouseEvent e) { arrastando = false; }
			@Override
			public void mousePressed(MouseEvent e) {
				arrastando = true;
				ultimoMouseX = e.getX();
				ultimoMouseY = e.getY();
				requestFocusInWindow();
			}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseClicked(MouseEvent e) {}
		});

		addMouseMotionListener(new MouseMotionListener() {
			@Override public void mouseMoved(MouseEvent e) {}

			@Override
			public void mouseDragged(MouseEvent e) {
				int dx = e.getX() - ultimoMouseX;
				int dy = e.getY() - ultimoMouseY;
				ultimoMouseX = e.getX();
				ultimoMouseY = e.getY();

				camera.yaw += dx * VEL_OLHAR_MOUSE;
				camera.pitch += dy * VEL_OLHAR_MOUSE;
				camera.clampPitch();
			}
		});
	}

	private void trataTecla(int key, boolean pressionado) {
		switch (key) {
			case KeyEvent.VK_W: moveFrente = pressionado; break;
			case KeyEvent.VK_S: moveTras = pressionado; break;
			case KeyEvent.VK_A: moveEsquerda = pressionado; break;
			case KeyEvent.VK_D: moveDireita = pressionado; break;
			case KeyEvent.VK_SPACE: moveCima = pressionado; break;
			case KeyEvent.VK_SHIFT: moveBaixo = pressionado; break;
			case KeyEvent.VK_LEFT: olharEsquerda = pressionado; break;
			case KeyEvent.VK_RIGHT: olharDireita = pressionado; break;
			case KeyEvent.VK_UP: olharCima = pressionado; break;
			case KeyEvent.VK_DOWN: olharBaixo = pressionado; break;
		}
	}

	private void carregarCena() {
		Object[][] cena = {
				{ "medieval house.obj", -18f, 9f },
				{ "chair_01.obj", -10f, 2.5f },
				{ "tank.obj", -5f, 4f },
				{ "AIM120D.obj", 0f, 5f },
				{ "Bench_LowRes.obj", 6f, 4f },
				{ "mig21_fishbed.obj", 13f, 7f },
				{ "SR71.obj", 22f, 9f },
				{ "x-35_obj.obj", 32f, 7f },
		};

		for (Object[] item : cena) {
			String arquivo = (String) item[0];
			float x = (Float) item[1];
			float tamanho = (Float) item[2];
			try {
				ObjModel modelo = ObjLoader.carregar(arquivo);
				objetos.add(new SceneObject(arquivo, modelo, x, 0f, 0f, tamanho));
				System.out.println("Carregado " + arquivo + ": " + modelo.vertices.length + " vertices, "
						+ modelo.faces.length + " faces");
			} catch (Exception e) {
				System.err.println("Falha ao carregar " + arquivo + ": " + e.getMessage());
			}
		}
	}

	public void simulaMundo(long diftimeMs) {
		float dt = diftimeMs / 1000f;

		float[] frente = camera.forwardXZ();
		float[] direita = camera.rightXZ();

		float vx = 0, vy = 0, vz = 0;
		if (moveFrente) { vx += frente[0]; vz += frente[2]; }
		if (moveTras) { vx -= frente[0]; vz -= frente[2]; }
		if (moveDireita) { vx += direita[0]; vz += direita[2]; }
		if (moveEsquerda) { vx -= direita[0]; vz -= direita[2]; }
		if (moveCima) { vy += 1; }
		if (moveBaixo) { vy -= 1; }

		camera.x += vx * VEL_MOVIMENTO * dt;
		camera.y += vy * VEL_MOVIMENTO * dt;
		camera.z += vz * VEL_MOVIMENTO * dt;

		if (olharEsquerda) camera.yaw -= VEL_OLHAR_TECLADO * dt;
		if (olharDireita) camera.yaw += VEL_OLHAR_TECLADO * dt;
		if (olharCima) camera.pitch -= VEL_OLHAR_TECLADO * dt;
		if (olharBaixo) camera.pitch += VEL_OLHAR_TECLADO * dt;
		camera.clampPitch();
	}

	@Override
	public void paint(Graphics g0) {
		Graphics2D g = (Graphics2D) g0;

		g.setColor(Color.white);
		g.fillRect(0, 0, W, H);

		float focal = (H / 2f) / (float) Math.tan(Math.toRadians(FOV_GRAUS / 2f));
		Mat4x4 view = camera.getViewMatrix();

		g.setColor(Color.black);
		for (SceneObject obj : objetos) {
			Ponto3D[] verticesView = new Ponto3D[obj.verticesMundo.length];
			for (int i = 0; i < obj.verticesMundo.length; i++) {
				verticesView[i] = obj.verticesMundo[i].multiplicadoPor(view);
			}

			for (int[] face : obj.faces) {
				Ponto3D p0 = verticesView[face[0]];
				Ponto3D p1 = verticesView[face[1]];
				Ponto3D p2 = verticesView[face[2]];

				if (p0.z <= NEAR || p1.z <= NEAR || p2.z <= NEAR) continue;

				float s0 = focal / p0.z;
				float s1 = focal / p1.z;
				float s2 = focal / p2.z;

				int x0 = Math.round(W / 2f + p0.x * s0);
				int y0 = Math.round(H / 2f - p0.y * s0);
				int x1 = Math.round(W / 2f + p1.x * s1);
				int y1 = Math.round(H / 2f - p1.y * s1);
				int x2 = Math.round(W / 2f + p2.x * s2);
				int y2 = Math.round(H / 2f - p2.y * s2);

				g.drawLine(x0, y0, x1, y1);
				g.drawLine(x1, y1, x2, y2);
				g.drawLine(x2, y2, x0, y0);
			}
		}

		desenhaHud(g);
	}

	private void desenhaHud(Graphics2D g) {
		g.setFont(fonteHud);
		g.setColor(Color.black);
		g.drawString(String.format("FPS: %d  |  Camera: (%.1f, %.1f, %.1f)", fps, camera.x, camera.y, camera.z), 10, 20);
		g.drawString("WASD move | SPACE/SHIFT sobe/desce | setas ou arrastar mouse olham", 10, 40);
	}

	public void start() {
		runner = new Thread(this);
		runner.start();
	}

	@Override
	public void run() {
		long time = System.currentTimeMillis();
		long segundo = time / 1000;
		long diftime = 0;
		int quadros = 0;

		while (ativo) {
			simulaMundo(diftime);
			paintImmediately(0, 0, W, H);

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			long newtime = System.currentTimeMillis();
			diftime = newtime - time;
			time = newtime;

			quadros++;
			long novoSegundo = newtime / 1000;
			if (novoSegundo != segundo) {
				fps = quadros;
				quadros = 0;
				segundo = novoSegundo;
			}
		}
	}
}
