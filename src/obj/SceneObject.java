package obj;

import java.awt.Color;

import core3d.Ponto3D;

public class SceneObject {
	public String nome;
	public Ponto3D[] verticesMundo;
	public int[][] faces;
	public Color cor;

	public SceneObject(String nome, ObjModel modelo, float px, float py, float pz, float tamanhoAlvo, Color cor) {
		this.nome = nome;
		this.faces = modelo.faces;
		this.cor = cor;

		float maior = modelo.maiorDimensao();
		float escala = maior > 0.0001f ? (tamanhoAlvo / maior) : 1f;

		float cx = modelo.centroX();
		float cy = modelo.minY;
		float cz = modelo.centroZ();

		verticesMundo = new Ponto3D[modelo.vertices.length];
		for (int i = 0; i < modelo.vertices.length; i++) {
			Ponto3D v = modelo.vertices[i];
			float x = (v.x - cx) * escala + px;
			float y = (v.y - cy) * escala + py;
			float z = (v.z - cz) * escala + pz;
			verticesMundo[i] = new Ponto3D(x, y, z);
		}
	}
}
