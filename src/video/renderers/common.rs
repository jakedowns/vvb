use crate::emulator::video::{Eye, FrameBufferConsumers};
use anyhow::Result;

pub trait RenderLogic {
    fn init(&mut self) -> Result<()>;
    fn resize(&mut self, screen_size: (i32, i32)) -> Result<()>;
    fn update(&mut self, eye: Eye, buffer: &[u8]) -> Result<()>;
    fn draw(&self) -> Result<()>;
    fn request_change_mode(&mut self, _enable3d: bool) -> Result<()> {Ok(())}
}


pub struct Renderer<TLogic: RenderLogic> {
    frame_buffers: FrameBufferConsumers,
    pub logic: TLogic,
}
impl<TLogic: RenderLogic> Renderer<TLogic> {
    pub fn new(frame_buffers: FrameBufferConsumers, logic: TLogic) -> Self {
        Self {
            frame_buffers,
            logic,
        }
    }

    pub fn on_surface_created(&mut self) -> Result<()> {
        self.logic.init()
    }

    pub fn on_surface_changed(&mut self, width: i32, height: i32) -> Result<()> {
        self.logic.resize((width, height))
    }

    pub fn on_draw_frame(&mut self) -> Result<()> {
        self.update_eye(Eye::Left);
        self.update_eye(Eye::Right);
        self.logic.draw()
    }

    fn update_eye(&mut self, eye: Eye) {
        let logic = &mut self.logic;
        self.frame_buffers[eye].try_read(|data| {
            if logic.update(eye, data).is_err() {
                log::error!("Error updating eye!");
            }
        });
    }

    pub fn on_mode_changed(&mut self, enable3d: bool) -> Result<()> {
        self.logic.request_change_mode(enable3d)?;
        Ok(())
    }
}
