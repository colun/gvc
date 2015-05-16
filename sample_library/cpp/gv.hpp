#ifndef UTIL_GV_H
#define UTIL_GV_H

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#ifndef NDEBUG
#include <cstdarg>
#include <unistd.h>

using namespace std;

struct GV_RGB {
	int r;
	int g;
	int b;
	int toInt() const {
		return ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
	}
};

GV_RGB gvRGB(int r, int g, int b) {
	GV_RGB result;
	result.r = r;
	result.g = g;
	result.b = b;
	return result;
}

const GV_RGB & gvColor(int i) {
	static GV_RGB colors[] = { gvRGB(255, 0, 0), gvRGB(0, 255, 0), gvRGB(0, 0, 255), gvRGB(192, 192, 0), gvRGB(0, 255, 255), gvRGB(255, 0, 255), gvRGB(255, 128, 0), gvRGB(255, 0, 128) };
	assert(i < (int)(sizeof(colors)/sizeof(colors[0])));
	return colors[i];
}

FILE * g_gvFile = NULL;
bool g_gvEnableFlag = true;
bool g_gvSocketConnect = false;
void gvSetEnable(bool enable) {
	g_gvEnableFlag = enable;
}
void gvSetEnable(bool enable, bool & before) {
	before = g_gvEnableFlag;
	g_gvEnableFlag = enable;
}
void gvClose() {
	if (g_gvFile != NULL) {
		fclose(g_gvFile);
		g_gvFile = NULL;
		g_gvSocketConnect = false;
	}
}
#include <sys/socket.h>
#include <netdb.h>
void gvConnect(const char * host = "localhost", int port = 11111) {
	gvClose();
	hostent * servhost = gethostbyname(host);
	sockaddr_in server;
	bzero(&server, sizeof(server));
	server.sin_family = AF_INET;
	bcopy(servhost->h_addr, &server.sin_addr, servhost->h_length);
	server.sin_port = htons(port);
	int s = socket(AF_INET, SOCK_STREAM, 0);
	if(0<s) {
		int connect_ret = connect(s, (sockaddr*)&server, sizeof(server));
		if(connect_ret!=-1) {
			g_gvFile = fdopen(s, "r+");
			assert(g_gvFile != NULL);
			g_gvSocketConnect = true;
		}
	}
	if(!g_gvSocketConnect) {
		info("GV Connection Error\n");
	}
}
void gvCreate(const char * path) {
	gvClose();
	g_gvFile = fopen(path, "w+");
}
void gvInit() {
	if(g_gvFile==NULL) {
		gvCreate("result.gv");
	}
}
void gvInput(double & turn, double & x, double & y) {
	assert(g_gvEnableFlag);
	assert(g_gvSocketConnect);
	fprintf(g_gvFile, "i\n");
	fflush(g_gvFile);
	char buf[1024];
	char * ret1 = fgets(buf, sizeof(buf), g_gvFile);
	assert(ret1!=NULL);
	int ret2 = sscanf(buf, "%lf%lf%lf", &turn, &x, &y);
	assert(ret2==3);
}
void gvInput(int & turn, int & x, int & y) {
	double a, b, c;
	gvInput(a, b, c);
	turn = (int)lround(a);
	x = (int)lround(b);
	y = (int)lround(c);
}
void gvInput(int & turn, double & x, double & y) {
	double a;
	gvInput(a, x, y);
	turn = (int)lround(a);
}
void gvInput(double & turn, int & x, int & y) {
	double b, c;
	gvInput(turn, b, c);
	x = (int)lround(b);
	y = (int)lround(c);
}
void gvInput() {
	double a, b, c;
	gvInput(a, b, c);
}
void gvAutoMode() {
	assert(g_gvEnableFlag);
	assert(g_gvSocketConnect);
	fprintf(g_gvFile, "a\n");
	fflush(g_gvFile);
}
void gvCircle(double x, double y) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "c %g %g\n", x, y);
	fflush(g_gvFile);
}
void gvCircle(double x, double y, double r, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "c %g %g %d %g\n", x, y, rgb.toInt(), r);
	fflush(g_gvFile);
}
void gvCircle(double x, double y, double r) {
	if(!g_gvEnableFlag)return;
	gvCircle(x, y, r, gvRGB(0, 0, 0));
}
void gvCircle(double x, double y, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "c %g %g %d\n", x, y, rgb.toInt());
	fflush(g_gvFile);
}
void gvText(double x, double y, double r, GV_RGB rgb, const char * format = "?", ...) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "t %g %g %d %g ", x, y, rgb.toInt(), r);
	va_list arg;
	va_start(arg, format);
	vfprintf(g_gvFile, format, arg);
	va_end(arg);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvText(double x, double y, double r, const char * format = "?", ...) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "t %g %g 0 %g ", x, y, r);
	va_list arg;
	va_start(arg, format);
	vfprintf(g_gvFile, format, arg);
	va_end(arg);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvText(double x, double y, GV_RGB rgb, const char * format = "?", ...) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "t %g %g %d 0.5 ", x, y, rgb.toInt());
	va_list arg;
	va_start(arg, format);
	vfprintf(g_gvFile, format, arg);
	va_end(arg);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvText(double x, double y, const char * format = "?", ...) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "t %g %g 0 0.5 ", x, y);
	va_list arg;
	va_start(arg, format);
	vfprintf(g_gvFile, format, arg);
	va_end(arg);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvImage(double x, double y, double w, double h, const char * format = "", ...) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "b %g %g %g %g ", x, y, w, h);
	va_list arg;
	va_start(arg, format);
	vfprintf(g_gvFile, format, arg);
	va_end(arg);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvRect(double x, double y, double w, double h, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "p %d", rgb.toInt());
	fprintf(g_gvFile, " %g %g", x, y);
	fprintf(g_gvFile, " %g %g", x+w, y);
	fprintf(g_gvFile, " %g %g", x+w, y+h);
	fprintf(g_gvFile, " %g %g", x, y+h);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvRect(double x, double y, double w, double h) {
	gvRect(x, y, w, h, gvRGB(0, 0, 0));
}
void gvLine(double x1, double y1, double x2, double y2, double r, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvInit();
	double odx = x2-x1;
	double ody = y2-y1;
	double rate = r / sqrt(odx*odx+ody*ody);
	double dx = odx * rate;
	double dy = ody * rate;
	fprintf(g_gvFile, "p %d", rgb.toInt());
	fprintf(g_gvFile, " %g %g", x2-dy*(0.05/(1+sqrt(2))), y2+dx*(0.05/(1+sqrt(2))));
	fprintf(g_gvFile, " %g %g", x2-dx*(0.05*sqrt(2)/(1+sqrt(2)))-dy*0.05, y2-dy*(0.05*sqrt(2)/(1+sqrt(2)))+dx*0.05);
	fprintf(g_gvFile, " %g %g", x1+dx*(0.05*sqrt(2)/(1+sqrt(2)))-dy*0.05, y1+dy*(0.05*sqrt(2)/(1+sqrt(2)))+dx*0.05);
	fprintf(g_gvFile, " %g %g", x1-dy*(0.05/(1+sqrt(2))), y1+dx*(0.05/(1+sqrt(2))));
	fprintf(g_gvFile, " %g %g", x1+dy*(0.05/(1+sqrt(2))), y1-dx*(0.05/(1+sqrt(2))));
	fprintf(g_gvFile, " %g %g", x1+dx*(0.05*sqrt(2)/(1+sqrt(2)))+dy*0.05, y1+dy*(0.05*sqrt(2)/(1+sqrt(2)))-dx*0.05);
	fprintf(g_gvFile, " %g %g", x2-dx*(0.05*sqrt(2)/(1+sqrt(2)))+dy*0.05, y2-dy*(0.05*sqrt(2)/(1+sqrt(2)))-dx*0.05);
	fprintf(g_gvFile, " %g %g", x2+dy*(0.05/(1+sqrt(2))), y2-dx*(0.05/(1+sqrt(2))));
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvLine(double x1, double y1, double x2, double y2, double r) {
	gvLine(x1, y1, x2, y2, r, gvRGB(0, 0, 0));
}
void gvLine(double x1, double y1, double x2, double y2, GV_RGB rgb) {
	gvLine(x1, y1, x2, y2, 0.5, rgb);
}
void gvLine(double x1, double y1, double x2, double y2) {
	gvLine(x1, y1, x2, y2, 0.5);
}
double g_gvLastLineX;
double g_gvLastLineY;
void gvMoveTo(double x, double y) {
	g_gvLastLineX = x;
	g_gvLastLineY = y;
}
void gvLineTo(double x, double y, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvLine(g_gvLastLineX, g_gvLastLineY, x, y, rgb);
	g_gvLastLineX = x;
	g_gvLastLineY = y;
}
void gvLineTo(double x, double y) {
	if(!g_gvEnableFlag)return;
	gvLine(g_gvLastLineX, g_gvLastLineY, x, y);
	g_gvLastLineX = x;
	g_gvLastLineY = y;
}
void gvArrow(double x1, double y1, double x2, double y2, double r, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvInit();
	double odx = x2-x1;
	double ody = y2-y1;
	double rate = r / sqrt(odx*odx+ody*ody);
	double dx = odx * rate;
	double dy = ody * rate;
	double x2_base = x2+dx*0.1;
	double y2_base = y2+dy*0.1;
	double dx0 = dx*0.1*tan(M_PI*15/180);
	double dy0 = dy*0.1*tan(M_PI*15/180);
	double x2_3 = x2_base-dx*(0.1/sin(M_PI*15/180));
	double y2_3 = y2_base-dy*(0.1/sin(M_PI*15/180));
	double x2_4 = x2_3-dx*(0.05/tan(M_PI*15/180));
	double y2_4 = y2_3-dy*(0.05/tan(M_PI*15/180));
	double x2_5 = x2_base-dx*(1.0*cos(M_PI*15/180));
	double y2_5 = y2_base-dy*(1.0*cos(M_PI*15/180));
	double x2_6 = x2_5-dx*(0.1*sin(M_PI*15/180));
	double y2_6 = y2_5-dy*(0.1*sin(M_PI*15/180));
	double dx5 = dx*(1.0*sin(M_PI*15/180));
	double dy5 = dy*(1.0*sin(M_PI*15/180));
	double dx6 = dx5-dx*(0.1*cos(M_PI*15/180));
	double dy6 = dy5-dy*(0.1*cos(M_PI*15/180));
	fprintf(g_gvFile, "p %d", rgb.toInt());
	fprintf(g_gvFile, " %g %g", x2-dy0, y2+dx0);
	fprintf(g_gvFile, " %g %g", x2_5-dy5, y2_5+dx5);
	fprintf(g_gvFile, " %g %g", x2_6-dy6, y2_6+dx6);
	fprintf(g_gvFile, " %g %g", x2_4-dy*0.05, y2_4+dx*0.05);
	fprintf(g_gvFile, " %g %g", x1+dx*(0.05*sqrt(2)/(1+sqrt(2)))-dy*0.05, y1+dy*(0.05*sqrt(2)/(1+sqrt(2)))+dx*0.05);
	fprintf(g_gvFile, " %g %g", x1-dy*(0.05/(1+sqrt(2))), y1+dx*(0.05/(1+sqrt(2))));
	fprintf(g_gvFile, " %g %g", x1+dy*(0.05/(1+sqrt(2))), y1-dx*(0.05/(1+sqrt(2))));
	fprintf(g_gvFile, " %g %g", x1+dx*(0.05*sqrt(2)/(1+sqrt(2)))+dy*0.05, y1+dy*(0.05*sqrt(2)/(1+sqrt(2)))-dx*0.05);
	fprintf(g_gvFile, " %g %g", x2_4+dy*0.05, y2_4-dx*0.05);
	fprintf(g_gvFile, " %g %g", x2_6+dy6, y2_6-dx6);
	fprintf(g_gvFile, " %g %g", x2_5+dy5, y2_5-dx5);
	fprintf(g_gvFile, " %g %g", x2+dy0, y2-dx0);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvArrow(double x1, double y1, double x2, double y2, double r) {
	gvArrow(x1, y1, x2, y2, r, gvRGB(0, 0, 0));
}
void gvArrow(double x1, double y1, double x2, double y2, GV_RGB rgb) {
	gvArrow(x1, y1, x2, y2, 0.5, rgb);
}
void gvArrow(double x1, double y1, double x2, double y2) {
	gvArrow(x1, y1, x2, y2, 0.5);
}
void gvArrowTo(double x, double y, double r, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvArrow(g_gvLastLineX, g_gvLastLineY, x, y, r, rgb);
	g_gvLastLineX = x;
	g_gvLastLineY = y;
}
void gvArrowTo(double x, double y, double r) {
	gvArrowTo(x, y, r, gvRGB(0, 0, 0));
}
void gvArrowTo(double x, double y, GV_RGB rgb) {
	gvArrowTo(x, y, 0.5, rgb);
}
void gvArrowTo(double x, double y) {
	gvArrowTo(x, y, 0.5);
}
void gvArrowFrom(double x, double y, double r, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvArrow(x, y, g_gvLastLineX, g_gvLastLineY, rgb);
	g_gvLastLineX = x;
	g_gvLastLineY = y;
}
void gvArrowFrom(double x, double y, double r) {
	gvArrowTo(x, y, r, gvRGB(0, 0, 0));
}
void gvArrowFrom(double x, double y, GV_RGB rgb) {
	gvArrowTo(x, y, 0.5, rgb);
}
void gvArrowFrom(double x, double y) {
	gvArrowTo(x, y, 0.5);
}
void gvArrow2(double x1, double y1, double x2, double y2, double r, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvInit();
	double odx = x2-x1;
	double ody = y2-y1;
	double rate = r / sqrt(odx*odx+ody*ody);
	double dx = odx * rate;
	double dy = ody * rate;
	double x1_base = x1-dx*0.1;
	double y1_base = y1-dy*0.1;
	double x2_base = x2+dx*0.1;
	double y2_base = y2+dy*0.1;
	double dx0 = dx*0.1*tan(M_PI*15/180);
	double dy0 = dy*0.1*tan(M_PI*15/180);
	double x2_3 = x2_base-dx*(0.1/sin(M_PI*15/180));
	double y2_3 = y2_base-dy*(0.1/sin(M_PI*15/180));
	double x2_4 = x2_3-dx*(0.05/tan(M_PI*15/180));
	double y2_4 = y2_3-dy*(0.05/tan(M_PI*15/180));
	double x2_5 = x2_base-dx*(1.0*cos(M_PI*15/180));
	double y2_5 = y2_base-dy*(1.0*cos(M_PI*15/180));
	double x2_6 = x2_5-dx*(0.1*sin(M_PI*15/180));
	double y2_6 = y2_5-dy*(0.1*sin(M_PI*15/180));
	double x1_3 = x1_base+dx*(0.1/sin(M_PI*15/180));
	double y1_3 = y1_base+dy*(0.1/sin(M_PI*15/180));
	double x1_4 = x1_3+dx*(0.05/tan(M_PI*15/180));
	double y1_4 = y1_3+dy*(0.05/tan(M_PI*15/180));
	double x1_5 = x1_base+dx*(1.0*cos(M_PI*15/180));
	double y1_5 = y1_base+dy*(1.0*cos(M_PI*15/180));
	double x1_6 = x1_5+dx*(0.1*sin(M_PI*15/180));
	double y1_6 = y1_5+dy*(0.1*sin(M_PI*15/180));
	double dx5 = dx*(1.0*sin(M_PI*15/180));
	double dy5 = dy*(1.0*sin(M_PI*15/180));
	double dx6 = dx5-dx*(0.1*cos(M_PI*15/180));
	double dy6 = dy5-dy*(0.1*cos(M_PI*15/180));
	fprintf(g_gvFile, "p %d", rgb.toInt());
	fprintf(g_gvFile, " %g %g", x2-dy0, y2+dx0);
	fprintf(g_gvFile, " %g %g", x2_5-dy5, y2_5+dx5);
	fprintf(g_gvFile, " %g %g", x2_6-dy6, y2_6+dx6);
	fprintf(g_gvFile, " %g %g", x2_4-dy*0.05, y2_4+dx*0.05);
	fprintf(g_gvFile, " %g %g", x1_4-dy*0.05, y1_4+dx*0.05);
	fprintf(g_gvFile, " %g %g", x1_6-dy6, y1_6+dx6);
	fprintf(g_gvFile, " %g %g", x1_5-dy5, y1_5+dx5);
	fprintf(g_gvFile, " %g %g", x1-dy0, y1+dx0);
	fprintf(g_gvFile, " %g %g", x1+dy0, y1-dx0);
	fprintf(g_gvFile, " %g %g", x1_5+dy5, y1_5-dx5);
	fprintf(g_gvFile, " %g %g", x1_6+dy6, y1_6-dx6);
	fprintf(g_gvFile, " %g %g", x1_4+dy*0.05, y1_4-dx*0.05);
	fprintf(g_gvFile, " %g %g", x2_4+dy*0.05, y2_4-dx*0.05);
	fprintf(g_gvFile, " %g %g", x2_6+dy6, y2_6-dx6);
	fprintf(g_gvFile, " %g %g", x2_5+dy5, y2_5-dx5);
	fprintf(g_gvFile, " %g %g", x2+dy0, y2-dx0);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
void gvArrow2(double x1, double y1, double x2, double y2, double r) {
	gvArrow2(x1, y1, x2, y2, r, gvRGB(0, 0, 0));
}
void gvArrow2(double x1, double y1, double x2, double y2, GV_RGB rgb) {
	gvArrow2(x1, y1, x2, y2, 0.5, rgb);
}
void gvArrow2(double x1, double y1, double x2, double y2) {
	gvArrow2(x1, y1, x2, y2, 0.5);
}
void gvArrowFromTo(double x, double y, double r, GV_RGB rgb) {
	if(!g_gvEnableFlag)return;
	gvArrow2(g_gvLastLineX, g_gvLastLineY, x, y, r, rgb);
	g_gvLastLineX = x;
	g_gvLastLineY = y;
}
void gvArrowFromTo(double x, double y, double r) {
	gvArrowFromTo(x, y, r, gvRGB(0, 0, 0));
}
void gvArrowFromTo(double x, double y, GV_RGB rgb) {
	gvArrowFromTo(x, y, 0.5, rgb);
}
void gvArrowFromTo(double x, double y) {
	gvArrowFromTo(x, y, 0.5);
}
void gvOutput(const char * format, ...) {
	if(!g_gvEnableFlag)return;
	gvInit();
	fprintf(g_gvFile, "o ");
	va_list arg;
	va_start(arg, format);
	vfprintf(g_gvFile, format, arg);
	va_end(arg);
	fprintf(g_gvFile, "\n");
	fflush(g_gvFile);
}
long g_gvNewTimeOffset = 0;
void gvNewTime(double time) {
	if(!g_gvEnableFlag)return;
	gvInit();
	g_gvNewTimeOffset = ftell(g_gvFile);
	fprintf(g_gvFile, "n %g\n", time);
	fflush(g_gvFile);
}
void gvNewTime() {
	if(!g_gvEnableFlag)return;
	gvInit();
	g_gvNewTimeOffset = ftell(g_gvFile);
	fprintf(g_gvFile, "n\n");
	fflush(g_gvFile);
}
void gvRollback() {
	if(!g_gvEnableFlag)return;
	if(g_gvSocketConnect) {
		fprintf(g_gvFile, "r\n");
		fflush(g_gvFile);
	}
	else {
		ftruncate(fileno(g_gvFile), g_gvNewTimeOffset);
		fseek(g_gvFile, g_gvNewTimeOffset, SEEK_SET);
	}
}
void gvRollbackAll() {
	if(!g_gvEnableFlag)return;
	if(g_gvSocketConnect) {
		fprintf(g_gvFile, "ra\n");
		fflush(g_gvFile);
	}
	else {
		g_gvNewTimeOffset = 0;
		ftruncate(fileno(g_gvFile), g_gvNewTimeOffset);
		fseek(g_gvFile, g_gvNewTimeOffset, SEEK_SET);
	}
}
#else
#define gvRGB(...)
#define gvSetEnable(...)
#define gvClose(...)
#define gvConnect(...)
#define gvCreate(...)
#define gvInit(...)
#define gvInput(...)
#define gvCircle(...)
#define gvText(...)
#define gvImage(...)
#define gvRect(...)
#define gvLine(...)
#define gvArrow(...)
#define gvArrow2(...)
#define gvMoveTo(...)
#define gvLineTo(...)
#define gvOutput(...)
#define gvText(...)
#define gvNewTime(...)
#define gvRollback(...)
#define gvRollbackAll(...)
#endif

#endif
