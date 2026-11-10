package vn.lichhoc.cnt125;

import java.util.ArrayList;
import java.util.List;

/** Danh sách lớp TA.CĐ.CNT.1.25 nạp sẵn (từ file danh sách trực lớp). Sửa được trong app. */
public class Roster {
    static final String[][] DEFAULT = {
            {"CNT25T2001", "1", "Lê Nguyễn Thành", "An"},
            {"CNT25T2002", "2", "Đặng Trâm", "Anh"},
            {"CNT25T2003", "3", "Đoàn Thanh", "Bảo"},
            {"CNT25T2006", "4", "Hồ Quốc", "Cường"},
            {"CNT25T2008", "6", "Lâm Hoàng", "Đệ"},
            {"CNT25T2009", "7", "Phạm Trung", "Đông"},
            {"CNT25T2010", "8", "Hồ Hoàng", "Hiệp"},
            {"CNT25T2011", "9", "Hà Tuấn", "Hùng"},
            {"CNT25T2012", "10", "Lê Hoàng", "Huy"},
            {"CNT25T2014", "11", "Huỳnh Phạm Phú", "Khang"},
            {"CNT25T2015", "12", "Nguyễn Duy", "Khang"},
            {"CNT25T2017", "13", "Nguyễn Hữu", "Lộc"},
            {"CNT25T2019", "15", "Dương Trần Hoàng", "Ngân"},
            {"CNT25T2020", "16", "Lý Thanh", "Nguyên"},
            {"CNT25T2021", "17", "Dương Nguyễn Ngọc", "Nhi"},
            {"CNT25T2022", "18", "Nguyễn Minh", "Nhựt"},
            {"CNT25T2023", "19", "Phạm Nguyễn Tấn", "Phát"},
            {"CNT25T2024", "20", "Trần Hoàng", "Phong"},
            {"CNT25T2025", "21", "Nguyễn Trọng", "Phú"},
            {"CNT25T2026", "22", "Nguyễn Ngọc Minh", "Phương"},
            {"CNT25T2028", "23", "Huỳnh Văn", "Quân"},
            {"CNT25T2029", "24", "Lê Công", "Quí"},
            {"CNT25T2030", "25", "Ngô Xuân", "Quyết"},
            {"CNT25T2031", "26", "Lâm Minh", "Tài"},
            {"CNT25T2034", "28", "Cao Hoàng", "Thịnh"},
            {"CNT25T2035", "29", "Lê Phước", "Thịnh"},
            {"CNT25T2036", "30", "Phan Lê Anh", "Tiến"},
            {"CNT25T2037", "31", "Phạm Thanh", "Tú"},
            {"CNT25T2038", "32", "Trần Minh", "Tuấn"},
            {"CNT25T2039", "33", "Nguyễn Vĩnh", "Tường"},
            {"CNT25T2040", "34", "Nguyễn Công", "Tuyền"},
            {"CNT25T2041", "35", "Trần Hoàng", "Ân"},
            {"CNT25T2042", "36", "Trần Trí", "Kiệt"},
            {"CNT25T2056", "37", "Nguyễn Tấn", "Đạt"},
            {"SV38", "38", "Nguyễn Trường", "Giang"},
            {"SV39", "39", "Trần Thị Minh", "Thư"}
    };

    public static List<Duty.Student> defaults() {
        List<Duty.Student> out = new ArrayList<>();
        for (String[] r : DEFAULT) {
            Duty.Student s = new Duty.Student();
            s.id = r[0]; s.stt = Integer.parseInt(r[1]); s.ho = r[2]; s.ten = r[3];
            out.add(s);
        }
        return out;
    }
}
