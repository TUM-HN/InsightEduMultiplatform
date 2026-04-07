# Import the main Flask app
from REST.app import app

# Register blueprints
from REST.bot_manager.bot_survey import survey_bp
from REST.bot_manager.bot_controller import controller_bp
from REST.bot_manager.bot_attendance import attendance_bp
from REST.bot_manager.bot_server import server_bp
from REST.bot_manager.bot_role_controller import role_bp
from REST.bot_manager.bot_feedback import feedback_bp
from REST.bot_manager.settings_controller import settings_bp
from bot_manager.bot_ai import ai_bp
from bot_manager.bot_ai_statistics import stats_bp
from bot_manager.bot_ai_feedback_analysis import ai_feedback_analysis_bp

app.register_blueprint(survey_bp)
app.register_blueprint(controller_bp)
app.register_blueprint(attendance_bp)
app.register_blueprint(server_bp)
app.register_blueprint(role_bp)
app.register_blueprint(feedback_bp)
app.register_blueprint(settings_bp)
app.register_blueprint(ai_bp)
app.register_blueprint(stats_bp)
app.register_blueprint(ai_feedback_analysis_bp)

if __name__ == '__main__':
    app.run(host='0.0.0.0')
    # app.run(debug=True)
