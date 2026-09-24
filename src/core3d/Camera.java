package core3d;

public class Camera {
	public float x = 0, y = 2, z = -15;
	public float yaw = 0;
	public float pitch = 0;

	public static final float PITCH_MAX = 89f;

	public void clampPitch() {
		if (pitch > PITCH_MAX) pitch = PITCH_MAX;
		if (pitch < -PITCH_MAX) pitch = -PITCH_MAX;
	}

	public float[] forwardXZ() {
		float rad = (float) Math.toRadians(yaw);
		return new float[] { -(float) Math.sin(rad), 0, (float) Math.cos(rad) };
	}

	public float[] rightXZ() {
		float rad = (float) Math.toRadians(yaw);
		return new float[] { (float) Math.cos(rad), 0, (float) Math.sin(rad) };
	}

	public Mat4x4 getViewMatrix() {
		Mat4x4 t = new Mat4x4();
		t.setTranslate(-x, -y, -z);

		Mat4x4 ry = new Mat4x4();
		ry.setRotateY(-yaw);

		Mat4x4 rx = new Mat4x4();
		rx.setRotateX(-pitch);

		return rx.multiplica(ry).multiplica(t);
	}
}
