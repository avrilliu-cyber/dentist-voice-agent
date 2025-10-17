"use client";

import { useEffect, useState } from "react";
import axios from "axios";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

export default function Home() {
  const [patients, setPatients] = useState<any[]>([]);
  const [selected, setSelected] = useState<any | null>(null);

  // NEW: stats for the selected patient
  const [visitCount, setVisitCount] = useState<number | null>(null);
  const [firstTime, setFirstTime] = useState<boolean | null>(null);

  useEffect(() => {
    axios.get("http://localhost:8080/patients/unique").then((res) => {
      setPatients(res.data);
    });
  }, []);

  // NEW: fetch stats when opening the dialog
  const openDetails = async (p: any) => {
    setSelected(p);
    try {
      const resp = await axios.get(`http://localhost:8080/patients/${p.id}/stats`);
      setVisitCount(resp.data.visit_count);
      setFirstTime(resp.data.new_patient_first_time);
    } catch (e) {
      // keep UI resilient if stats endpoint fails
      setVisitCount(null);
      setFirstTime(null);
    }
  };

  // NEW: clear stats when closing dialog
  const closeDetails = () => {
    setSelected(null);
    setVisitCount(null);
    setFirstTime(null);
  };

  return (
    <main className="min-h-screen bg-gray-100 p-10">
      <div className="max-w-5xl mx-auto space-y-6">
        <h1 className="text-4xl font-bold text-center text-indigo-600">🦷 Dentist Voice Agent Dashboard</h1>
        <p className="text-center text-gray-600">Manage patients created via voice input</p>

        <Card className="shadow-lg">
          <CardContent className="p-6">
            <Table>
              <TableHeader>
                <TableRow className="bg-indigo-50">
                  <TableHead>First Name</TableHead>
                  <TableHead>Last Name</TableHead>
                  <TableHead>Phone</TableHead>
                  <TableHead>Address</TableHead>
                  <TableHead>New Patient</TableHead>
                  <TableHead></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {patients.length > 0 ? (
                  patients.map((p) => (
                    <TableRow key={p.id} className="hover:bg-gray-50 transition">
                      <TableCell>{p.firstName}</TableCell>
                      <TableCell>{p.lastName}</TableCell>
                      <TableCell>{p.phoneNumber}</TableCell>
                      <TableCell>{p.address}</TableCell>
                      <TableCell>
                        {p.newPatient ? (
                          <span className="text-green-600 font-medium">Yes</span>
                        ) : (
                          <span className="text-gray-400">No</span>
                        )}
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="outline"
                          className="text-indigo-600 border-indigo-200"
                          onClick={() => openDetails(p)} // CHANGED: call openDetails
                        >
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center text-gray-400 py-4">
                      No patients found.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Dialog open={!!selected} onOpenChange={closeDetails}> {/* CHANGED: clear stats on close */}
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Patient Details</DialogTitle>
            </DialogHeader>
            {selected && (
              <div className="space-y-3 text-gray-700">
                <p><strong>First Name:</strong> {selected.firstName}</p>
                <p><strong>Last Name:</strong> {selected.lastName}</p>
                <p><strong>Phone:</strong> {selected.phoneNumber}</p>
                <p><strong>Address:</strong> {selected.address}</p>
                <p>
                  <strong>New Patient:</strong>{" "}
                  {selected.newPatient ? (
                    <span className="text-green-600 font-medium">Yes</span>
                  ) : (
                    <span className="text-gray-400">No</span>
                  )}
                </p>

                {/* NEW: show total visit count + whether the very first visit was new */}
                {visitCount !== null && (
                  <p><strong>Visits:</strong> {visitCount}</p>
                )}
                {firstTime !== null && (
                  <p><strong>First-time New:</strong> {firstTime ? "Yes" : "No"}</p>
                )}
              </div>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </main>
  );
}
