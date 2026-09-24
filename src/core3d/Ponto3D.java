package core3d;

public class Ponto3D {
	public float x;
	public float y;
	public float z;
	public float w;

	public Ponto3D(Ponto3D p) {
		x = p.x;
		y = p.y;
		z = p.z;
		w = p.w;
	}

	public Ponto3D(float x, float y, float z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = 1;
	}

	public Ponto3D(float x, float y, float z, float w) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public Ponto3D multiplicadoPor(Mat4x4 mat) {
		float x1 = mat.mat[0][0] * x + mat.mat[0][1] * y + mat.mat[0][2] * z + mat.mat[0][3] * w;
		float y1 = mat.mat[1][0] * x + mat.mat[1][1] * y + mat.mat[1][2] * z + mat.mat[1][3] * w;
		float z1 = mat.mat[2][0] * x + mat.mat[2][1] * y + mat.mat[2][2] * z + mat.mat[2][3] * w;
		float w1 = mat.mat[3][0] * x + mat.mat[3][1] * y + mat.mat[3][2] * z + mat.mat[3][3] * w;

		return new Ponto3D(x1, y1, z1, w1);
	}

	public void multiplicaMat(Mat4x4 mat) {
		Ponto3D r = multiplicadoPor(mat);
		x = r.x;
		y = r.y;
		z = r.z;
		w = r.w;
	}
}
