package obj;

import core3d.Ponto3D;

public class ObjModel {
	public Ponto3D[] vertices;
	public int[][] faces;

	public float minX, minY, minZ;
	public float maxX, maxY, maxZ;

	public ObjModel(Ponto3D[] vertices, int[][] faces) {
		this.vertices = vertices;
		this.faces = faces;
		calculaBoundingBox();
	}

	private void calculaBoundingBox() {
		minX = minY = minZ = Float.MAX_VALUE;
		maxX = maxY = maxZ = -Float.MAX_VALUE;
		for (Ponto3D v : vertices) {
			if (v.x < minX) minX = v.x;
			if (v.y < minY) minY = v.y;
			if (v.z < minZ) minZ = v.z;
			if (v.x > maxX) maxX = v.x;
			if (v.y > maxY) maxY = v.y;
			if (v.z > maxZ) maxZ = v.z;
		}
	}

	public float centroX() { return (minX + maxX) / 2f; }
	public float centroY() { return (minY + maxY) / 2f; }
	public float centroZ() { return (minZ + maxZ) / 2f; }

	public float maiorDimensao() {
		float dx = maxX - minX;
		float dy = maxY - minY;
		float dz = maxZ - minZ;
		return Math.max(dx, Math.max(dy, dz));
	}
}
